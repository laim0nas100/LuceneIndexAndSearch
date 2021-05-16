package lt.lb.luceneindexandsearch.config.indexing;

import java.io.IOException;
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
public interface IndexingWriterConfig extends DocumentFieldsAware {

    public Analyzer getIndexingAnalyzer();

    public SyncDirectory getDirectory();

    public default MergeScheduler getMergeScheduler() {
        return new ConcurrentMergeScheduler();
    }

    public default MergePolicy getMergePolicy() {
        return new TieredMergePolicy();
    }

    public default IndexWriterConfig getIndexWriterConfig() {
        return new IndexWriterConfig(getIndexingAnalyzer())
                .setMergePolicy(getMergePolicy())
                .setMergeScheduler(getMergeScheduler());
    }

    public IndexWriter getIndexWriter() throws IOException;
}
