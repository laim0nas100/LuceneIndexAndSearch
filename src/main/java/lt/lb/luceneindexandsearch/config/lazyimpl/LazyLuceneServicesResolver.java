package lt.lb.luceneindexandsearch.config.lazyimpl;

import lt.lb.luceneindexandsearch.config.GrowingMultiIndexingConfig;
import lt.lb.luceneindexandsearch.config.LuceneServicesResolver;
import lt.lb.luceneindexandsearch.config.LuceneTaskExecutor;
import lt.lb.luceneindexandsearch.config.indexing.IndexingMultiReaderConfig;

/**
 *
 * @author laim0nas100
 */
public class LazyLuceneServicesResolver<P> implements LuceneServicesResolver<P> {

    protected GrowingMultiIndexingConfig<P> multiIndexingConfig;
    protected IndexingMultiReaderConfig multiReaderConfig;
    protected LuceneTaskExecutor luceneTaskExecutor;
    
    
    @Override
    public GrowingMultiIndexingConfig<P> getMultiIndexingConfig() {
        return multiIndexingConfig;
    }

    public void setMultiIndexingConfig(GrowingMultiIndexingConfig<P> multiIndexingConfig) {
        this.multiIndexingConfig = multiIndexingConfig;
    }

    @Override
    public IndexingMultiReaderConfig getMultiReaderConfig() {
        return multiReaderConfig;
    }

    public void setMultiReaderConfig(IndexingMultiReaderConfig multiReaderConfig) {
        this.multiReaderConfig = multiReaderConfig;
    }

    @Override
    public LuceneTaskExecutor getLuceneTaskExecutor() {
        return luceneTaskExecutor;
    }

    public void setLuceneTaskExecutor(LuceneTaskExecutor luceneTaskExecutor) {
        this.luceneTaskExecutor = luceneTaskExecutor;
    }

}
