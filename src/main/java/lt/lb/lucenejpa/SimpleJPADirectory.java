package lt.lb.lucenejpa;

import lt.lb.luceneindexandsearch.splitting.DirConfig;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import lt.lb.commons.Java;
import lt.lb.luceneindexandsearch.splitting.JpaDirConfig;
import lt.lb.lucenejpa.io.DatabaseIndexInput;
import lt.lb.lucenejpa.io.DatabaseIndexOutput;
import lt.lb.lucenejpa.io.DatabaseReadWriteLockFactory;
import lt.lb.lucenejpa.io.DatabateIndexTempOutput;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

/**
 *
 * @author laim0nas100
 */
public class SimpleJPADirectory extends Directory {

    protected AtomicLong nextTempFileCounter = new AtomicLong(Java.getCurrentTimeMillis());

    protected final JpaDirConfig conf;
    protected final LockFactory lockFactory;

    public SimpleJPADirectory(JpaDirConfig conf) {
        this.conf = conf;
        this.lockFactory = new DatabaseReadWriteLockFactory(conf);
    }

    @Override
    public String[] listAll() throws IOException {
        return Q.listAll(conf);
    }

    @Override
    public void deleteFile(String string) throws IOException {
        Q.deleteFile(conf, string);
    }

    @Override
    public long fileLength(String string) throws IOException {
        return Q.fileLength(conf, string);
    }

    @Override
    public IndexOutput createOutput(String string, IOContext ioc) throws IOException {
        return new DatabaseIndexOutput(conf, string, ioc);
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext ioc) throws IOException {
        IndexFileNames.segmentFileName(prefix, suffix + "_" + Long.toString(nextTempFileCounter.getAndIncrement(), Character.MAX_RADIX), "tmp");
        String name = prefix + "_" + conf.tempFileName(suffix) + ".tmp";
        return new DatabateIndexTempOutput(conf, name, ioc);
    }

    @Override
    public void sync(Collection<String> clctn) throws IOException {
        //no op
    }

    @Override
    public void syncMetaData() throws IOException {
        //no op
    }

    @Override
    public void rename(String src, String dst) throws IOException {
        if (!Q.renameFile(conf, src, dst)) {
            throw new IOException("Failed to rename file:" + conf.getFullName(src) + " -> " + conf.getFullName(dst));
        }
    }

    @Override
    public IndexInput openInput(String string, IOContext ioc) throws IOException {
        return new DatabaseIndexInput(conf, string, ioc);
//        return new NewInput(conf, string,"desc"+string, ioc);
//        return MyByteBufferIndexInput.newInstance(conf, string);
    }

    @Override
    public Lock obtainLock(String string) throws IOException {
        return lockFactory.obtainLock(this, string);
    }

    @Override
    public void close() throws IOException {
        //no op
    }

    static HashSet<String> emptySet = new HashSet<>();

    @Override
    public Set<String> getPendingDeletions() throws IOException {
        return emptySet;
    }

}
