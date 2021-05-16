package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lt.lb.commons.containers.caching.Dependency;
import lt.lb.commons.containers.caching.lazy.LazyProxy;
import lt.lb.commons.containers.caching.lazy.LazySettableValue;
import lt.lb.commons.containers.caching.lazy.LazyValueThreaded;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.indexing.IndexingReaderConfig;
import lt.lb.lucenejpa.SyncDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;

/**
 *
 * @author laim0nas100
 */
public class LazyIndexingReaderConfig implements IndexingReaderConfig {

    protected DocumentFieldsConfig documentFieldsConfig;
    protected Analyzer searchAnalyzer;

    protected LazyProxy<SyncDirectory> lazyDirectory = new LazySettableValue<SyncDirectory>(null) {
        @Override
        public <V> LazyProxy<V> createNew(Dependency<V> supl) {
            return new LazyValueThreaded<>(supl);
        }
    };

    protected LazyImp.LazyReaderProvider readerProvider;

    public LazyIndexingReaderConfig(Consumer<ThreadLocal> threadLocalCons) {
        readerProvider = new LazyImp.LazyReaderProvider(lazyDirectory);
        Stream.of(readerProvider.lazyDir, readerProvider.lazyIndexReader)
                .filter(f -> f instanceof LazyValueThreaded)
                .map(m -> (LazyValueThreaded) m)
                .forEach(lvt -> lvt.collectThreadLocal(threadLocalCons));
    }

    @Override
    public IndexReader getIndexReader() throws IOException {
        return readerProvider.get();
    }

    @Override
    public DocumentFieldsConfig getDocumentFieldsConfig() {
        return documentFieldsConfig;
    }

    @Override
    public void setDocumentFieldsConfig(DocumentFieldsConfig documentFieldsConfig) {
        this.documentFieldsConfig = documentFieldsConfig;
    }

    @Override
    public Analyzer getSearchAnalyzer() {
        return searchAnalyzer;
    }

    public void setSearchAnalyzer(Analyzer analyzer) {
        this.searchAnalyzer = analyzer;
    }

    @Override
    public SyncDirectory getDirectory() {
        return lazyDirectory.get();
    }

    public void setDirectory(SyncDirectory dir) {
        lazyDirectory.set(dir);
    }

}
