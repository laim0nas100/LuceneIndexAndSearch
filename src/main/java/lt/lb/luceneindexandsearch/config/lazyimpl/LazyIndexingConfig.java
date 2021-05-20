package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lt.lb.commons.containers.caching.Dependency;
import lt.lb.commons.containers.caching.lazy.LazyProxy;
import lt.lb.commons.containers.caching.lazy.LazySettableValue;
import lt.lb.commons.containers.caching.lazy.LazyValueThreaded;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.IndexingConfig;
import lt.lb.luceneindexandsearch.config.lazyimpl.LazyImp.LazyReaderProvider;
import lt.lb.luceneindexandsearch.config.lazyimpl.LazyImp.LazyWriterProvider;
import lt.lb.lucenejpa.SyncDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.TieredMergePolicy;

/**
 *
 * @author laim0nas100
 */
public class LazyIndexingConfig implements IndexingConfig {

    protected Analyzer indexingAnalyzer;
    protected Analyzer searchAnalyzer;
    protected DocumentFieldsConfig documentFieldsConfig;

    protected LazyProxy<SyncDirectory> lazyDirectory = new LazySettableValue<SyncDirectory>(null) {
        @Override
        public <V> LazyProxy<V> createNew(Dependency<V> supl) {
            return new LazyValueThreaded<>(supl);
        }
    };

    protected LazyReaderProvider readerProvider = new LazyReaderProvider(lazyDirectory);
    protected LazyWriterProvider writerProvider = new LazyWriterProvider(lazyDirectory, () -> getIndexWriterConfig());

    public LazyIndexingConfig(Consumer<ThreadLocal> threadLocalCons) {
        Stream.of(readerProvider.lazyDir, readerProvider.lazyIndexReader, writerProvider.lazyIndexWriter)
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
        return indexingAnalyzer;
    }

    @Override
    public Analyzer getSearchAnalyzer() {
        return searchAnalyzer;
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

    @Override
    public IndexReader getIndexReader() throws IOException {
        return readerProvider.get();
    }

    public void setIndexingAnalyzer(Analyzer indexingAnalyzer) {
        this.indexingAnalyzer = indexingAnalyzer;
    }

    public void setDirectory(SyncDirectory directory) {
        lazyDirectory.set(directory);
    }

    public void setSearchAnalyzer(Analyzer searchAnalyzer) {
        this.searchAnalyzer = searchAnalyzer;
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
