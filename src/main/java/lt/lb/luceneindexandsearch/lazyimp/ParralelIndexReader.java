package lt.lb.luceneindexandsearch.lazyimp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lt.lb.uncheckedutils.NestedException;
import lt.lb.commons.threads.executors.TaskBatcher;
import lt.lb.uncheckedutils.func.UncheckedFunction;
import org.apache.lucene.index.CompositeReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Term;

/**
 *
 * @author laim0nas100
 */
public class ParralelIndexReader<R extends IndexReader> extends CompositeReader {

    protected final boolean closeSubreaders;
    protected final R[] subReaders;
    protected final int[] starts;       // 1st docno for each reader
    protected final int maxDoc;
    protected int numDocs = -1;         // computed lazily
    protected final List<R> subReadersList;
    protected final Executor exe;

    public ParralelIndexReader(Executor exe, R[] subReaders, boolean closeSubreaders) throws IOException {
        this.subReaders = subReaders;
        this.exe = exe;
        this.subReadersList = Collections.unmodifiableList(Arrays.asList(subReaders));
        starts = new int[subReaders.length + 1];    // build starts array
        this.closeSubreaders = closeSubreaders;
        long max = 0;
        for (int i = 0; i < subReaders.length; i++) {
            starts[i] = (int) max;
            final IndexReader r = subReaders[i];
            max += r.maxDoc();      // compute maxDocs
            r.registerParentReader(this);
        }

        if (max > IndexWriter.MAX_DOCS) {
            // Caller is building a MultiReader and it has too many documents; this case is just illegal arguments:
            throw new IllegalArgumentException("Too many documents: composite IndexReaders cannot exceed " + IndexWriter.MAX_DOCS + " but readers have total maxDoc=" + max);
        }

        this.maxDoc = Math.toIntExact(max);
        starts[subReaders.length] = this.maxDoc;
    }

    @Override
    protected List<? extends IndexReader> getSequentialSubReaders() {
        return subReadersList;
    }

    @Override
    public Fields getTermVectors(int docID) throws IOException {
        final int i = readerIndex(docID);        // find subreader num
        return subReaders[i].getTermVectors(docID - starts[i]); // dispatch to subreader
    }

    @Override
    public int numDocs() {

        if (this.numDocs == -1) {
            int num = doBatchInt(r -> r.numDocs());
            assert num >= 0;
            numDocs = num;
        }
        return numDocs;
    }

    @Override
    public int maxDoc() {
        return maxDoc;
    }

    /**
     * Helper method for subclasses to get the corresponding reader for a doc ID
     */
    protected final int readerIndex(int docID) {
        if (docID < 0 || docID >= maxDoc) {
            throw new IllegalArgumentException("docID must be >= 0 and < maxDoc=" + maxDoc + " (got docID=" + docID + ")");
        }
        return ReaderUtil.subIndex(docID, this.starts);
    }

    /**
     * Helper method for subclasses to get the docBase of the given sub-reader
     * index.
     */
    protected final int readerBase(int readerIndex) {
        if (readerIndex < 0 || readerIndex >= subReaders.length) {
            throw new IllegalArgumentException("readerIndex must be >= 0 and < getSequentialSubReaders().size()");
        }
        return this.starts[readerIndex];
    }

    @Override
    public void document(int docID, StoredFieldVisitor visitor) throws IOException {
        ensureOpen();
        final int i = readerIndex(docID);                          // find subreader num
        subReaders[i].document(docID - starts[i], visitor);    // dispatch to subreader
    }

    @Override
    protected synchronized void doClose() throws IOException {
        IOException ioe = null;
        for (final IndexReader r : getSequentialSubReaders()) {
            try {
                if (closeSubreaders) {
                    r.close();
                } else {
                    r.decRef();
                }
            } catch (IOException e) {
                if (ioe == null) {
                    ioe = e;
                }
            }
        }
        // throw the first exception
        if (ioe != null) {
            throw ioe;
        }
    }

    @Override
    public CacheHelper getReaderCacheHelper() {
        if (getSequentialSubReaders().size() == 1) {
            return getSequentialSubReaders().get(0).getReaderCacheHelper();
        }
        return null;
    }

    @Override
    public int docFreq(Term term) throws IOException {
        return doBatchInt(r -> {
            int sub = r.docFreq(term);
            assert sub >= 0;
            assert sub <= r.getDocCount(term.field());
            return sub;
        });
    }

    @Override
    public long totalTermFreq(Term term) throws IOException {
        return doBatchLong(r -> {
            long sub = r.totalTermFreq(term);
            assert sub >= 0;
            assert sub <= r.getSumTotalTermFreq(term.field());
            return sub;
        });
    }

    @Override
    public long getSumDocFreq(String field) throws IOException {
        return doBatchLong(r -> {
            long sub = r.getSumDocFreq(field);
            assert sub >= 0;
            assert sub <= r.getSumTotalTermFreq(field);
            return sub;
        });
    }

    @Override

    public int getDocCount(String field) throws IOException {
        return doBatchInt(r -> {
            int sub = r.getDocCount(field);
            assert sub >= 0;
            assert sub <= r.maxDoc();
            return sub;
        });

    }

    @Override
    public long getSumTotalTermFreq(String field) throws IOException {
        return doBatchLong(r -> {
            long sub = r.getSumTotalTermFreq(field);
            assert sub >= 0;
            assert sub >= r.getSumDocFreq(field);
            return sub;
        });
    }

    protected long doBatchLong(UncheckedFunction<R, Long> func) {
        ensureOpen();
        TaskBatcher tasks = new TaskBatcher(exe);
        AtomicLong atomicTotal = new AtomicLong(0);// sum doc total term freqs in subreaders
        for (int i = 0; i < subReaders.length; i++) {
            final R reader = subReaders[i];
            tasks.execute(() -> {
                Long call = func.apply(reader);
                atomicTotal.addAndGet(call);
                return call;
            });
        }
        tasks.awaitFailOnFirst().failures.forEach(NestedException::nestedThrow);
        return atomicTotal.get();
    }

    protected int doBatchInt(UncheckedFunction<R, Integer> func) {
        ensureOpen();
        TaskBatcher tasks = new TaskBatcher(exe);
        AtomicInteger atomicTotal = new AtomicInteger(0);// sum doc total term freqs in subreaders
        for (int i = 0; i < subReaders.length; i++) {
            final R reader = subReaders[i];
            tasks.execute(() -> {
                Integer call = func.apply(reader);
                atomicTotal.addAndGet(call);
                return call;
            });
        }
        tasks.awaitFailOnFirst().failures.forEach(NestedException::nestedThrow);
        return atomicTotal.get();
    }

}
