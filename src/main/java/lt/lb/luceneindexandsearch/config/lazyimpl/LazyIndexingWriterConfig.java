package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lt.lb.commons.containers.caching.Dependency;
import lt.lb.commons.containers.caching.lazy.LazyProxy;
import lt.lb.commons.containers.caching.lazy.LazySettableValue;
import lt.lb.commons.containers.caching.lazy.LazyValueThreaded;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.indexing.IndexingWriterConfig;
import lt.lb.lucenejpa.SyncDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.TieredMergePolicy;

/**
 *
 * @author laim0nas100
 */
public class LazyIndexingWriterConfig implements IndexingWriterConfig {

    protected Analyzer analyzer;
    protected DocumentFieldsConfig documentFieldsConfig;
    protected LazySettableValue<SyncDirectory> lazyDirectory = new LazySettableValue<SyncDirectory>(null) {
        @Override
        public <V> LazyProxy<V> createNew(Dependency<V> supl) {
            return new LazyValueThreaded<>(supl);
        }
    };
    
     protected LazyImp.LazyWriterProvider writerProvider = new LazyImp.LazyWriterProvider(lazyDirectory, this::getIndexWriterConfig);

    public LazyIndexingWriterConfig(Consumer<ThreadLocal> threadLocalCons) {
        Stream.of(writerProvider.lazyDir, writerProvider.lazyIndexWriter)
                .filter(f -> f instanceof LazyValueThreaded)
                .map(m -> (LazyValueThreaded) m)
                .forEach(lvt -> lvt.collectThreadLocal(threadLocalCons));

    }

    protected MergeScheduler mergeScheduler = new ConcurrentMergeScheduler();
    protected MergePolicy mergePolicy = new TieredMergePolicy();

    @Override
    public MergeScheduler getMergeScheduler() {
        return mergeScheduler;
    }

    @Override
    public MergePolicy getMergePolicy() {
        return mergePolicy;
    }

    @Override
    public DocumentFieldsConfig getDocumentFieldsConfig() {
        return documentFieldsConfig;
    }

    @Override
    public SyncDirectory getDirectory() {
        return lazyDirectory.get();
    }

    @Override
    public Analyzer getIndexingAnalyzer() {
        return analyzer;
    }

    @Override
    public IndexWriterConfig getIndexWriterConfig() {
        return new IndexWriterConfig(getIndexingAnalyzer())
                .setMergePolicy(getMergePolicy())
                .setMergeScheduler(getMergeScheduler());
    }


    @Override
    public IndexWriter getIndexWriter() throws IOException {
        return writerProvider.get();
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void setDirectory(SyncDirectory directory) {
        lazyDirectory.set(directory);
    }

    @Override
    public void setDocumentFieldsConfig(DocumentFieldsConfig documentFieldsConfig) {
        this.documentFieldsConfig = documentFieldsConfig;
    }

    public void setMergeScheduler(MergeScheduler mergeScheduler) {
        this.mergeScheduler = mergeScheduler;
    }

    public void setMergePolicy(MergePolicy mergePolicy) {
        this.mergePolicy = mergePolicy;
    }

}
