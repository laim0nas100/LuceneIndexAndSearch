package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lt.lb.commons.Lazy;
import lt.lb.commons.containers.collections.CollectionOp;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.indexing.IndexingMultiReaderConfig;
import lt.lb.luceneindexandsearch.config.indexing.IndexingReaderConfig;
import lt.lb.lucenejpa.SyncDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;

/**
 *
 * @author laim0nas100
 */
public class LazyIndexingMultiReaderConfig implements IndexingMultiReaderConfig {

    protected Supplier<Collection<IndexingReaderConfig>> configSupplier;
    protected Lazy<Collection<IndexingReaderConfig>> configs = new Lazy<>(() -> {
        Objects.requireNonNull(configSupplier, "Config supplier is null");
        return configSupplier.get();
    });

    protected Lazy<IndexingReaderConfig> firstConfig = new Lazy<>(() -> {
        Collection<IndexingReaderConfig> get = configs.get();
        if (CollectionOp.isEmpty(get)) {
            throw new IllegalArgumentException("Configs are empty");
        }
        Optional<IndexingReaderConfig> findFirst = get.stream().filter(f -> f != null).findFirst();
        if (!findFirst.isPresent()) {
            throw new IllegalArgumentException("Failed to find non-null config");
        }
        return findFirst.get();
    });

    public LazyIndexingMultiReaderConfig() {
    }

    @Override
    public Collection<IndexingReaderConfig> getReaderConfigs() {
        return configSupplier.get();
    }

    public void setConfigSupplier(Supplier<Collection<IndexingReaderConfig>> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @Override
    public Analyzer getSearchAnalyzer() {
        return firstConfig.get().getSearchAnalyzer();
    }

    @Override
    public SyncDirectory getDirectory() {
        return firstConfig.get().getDirectory();
    }

    @Override
    public DocumentFieldsConfig getDocumentFieldsConfig() {
        return firstConfig.get().getDocumentFieldsConfig();
    }

    @Override
    public void setDocumentFieldsConfig(DocumentFieldsConfig documentFieldsConfig) {
        throw new UnsupportedOperationException("Not supported in multireader.");
    }

    @Override
    public IndexReader getIndexReader() throws IOException {
        return IndexingMultiReaderConfig.makeMultiReader(configs.get());
    }

}
