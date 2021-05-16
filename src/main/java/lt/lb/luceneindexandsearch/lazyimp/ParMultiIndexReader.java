package lt.lb.luceneindexandsearch.lazyimp;

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

/**
 *
 * @author laim0nas100
 */
public class ParMultiIndexReader extends MultiReader {

    public ParMultiIndexReader(IndexReader... subReaders) throws IOException {
        super(subReaders, false);
    }
    
    
    
    
}
