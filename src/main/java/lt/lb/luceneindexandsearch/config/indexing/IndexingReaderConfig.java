package lt.lb.luceneindexandsearch.config.indexing;

import java.io.IOException;
import java.util.Objects;
import lt.lb.commons.threads.sync.AsyncUtil;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.lucenejpa.SyncDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;

/**
 *
 * @author laim0nas100
 */
public interface IndexingReaderConfig extends DocumentFieldsAware {

    public Analyzer getSearchAnalyzer();

    public IndexReader getIndexReader() throws IOException;

    public SyncDirectory getDirectory();

    public static class Forwarding implements IndexingReaderConfig {

        protected IndexingReaderConfig config;

        public Forwarding(IndexingReaderConfig config) {
            this.config = Objects.requireNonNull(config);
        }

        @Override
        public Analyzer getSearchAnalyzer() {
            return config.getSearchAnalyzer();
        }

        @Override
        public IndexReader getIndexReader() throws IOException {
            return config.getIndexReader();
        }

        @Override
        public SyncDirectory getDirectory() {
            return config.getDirectory();
        }

        @Override
        public IndexingReaderConfig withNewReader(AsyncUtil.IOSupplier<IndexReader> readerSupl) {
            return config.withNewReader(readerSupl);
        }

        @Override
        public DocumentFieldsConfig getDocumentFieldsConfig() {
            return config.getDocumentFieldsConfig();
        }

        @Override
        public void setDocumentFieldsConfig(DocumentFieldsConfig documentFieldsConfig) {
            config.setDocumentFieldsConfig(documentFieldsConfig);
        }

    }

    public default IndexingReaderConfig withNewReader(AsyncUtil.IOSupplier<IndexReader> readerSupl) {
        Objects.requireNonNull(readerSupl);
        return new IndexingReaderConfig.Forwarding(this) {
            @Override
            public IndexReader getIndexReader() throws IOException {
                return readerSupl.get();
            }
        };
    }

}
