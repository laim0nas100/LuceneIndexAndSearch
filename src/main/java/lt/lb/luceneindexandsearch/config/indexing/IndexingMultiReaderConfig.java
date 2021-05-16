package lt.lb.luceneindexandsearch.config.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import lt.lb.luceneindexandsearch.lazyimp.ParralelIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

/**
 *
 * @author laim0nas100
 */
public interface IndexingMultiReaderConfig extends IndexingReaderConfig {

    public Collection<IndexingReaderConfig> getReaderConfigs();

    @Override
    public IndexReader getIndexReader() throws IOException;

    public static MultiReader makeMultiReader(Iterable<IndexingReaderConfig> configs) throws IOException {
        ArrayList<IndexReader> readers = new ArrayList<>();
        for (IndexingReaderConfig c : configs) {
            readers.add(c.getIndexReader());
        }

        return new MultiReader(readers.stream().toArray(s -> new IndexReader[s]));
    }

    public static ParralelIndexReader makeParralelReader(Executor exe, Iterable<IndexingReaderConfig> configs) throws IOException {
        ArrayList<IndexReader> readers = new ArrayList<>();
        for (IndexingReaderConfig c : configs) {
            readers.add(c.getIndexReader());
        }

        return new ParralelIndexReader(exe, readers.stream().toArray(s -> new IndexReader[s]), true);
    }

}
