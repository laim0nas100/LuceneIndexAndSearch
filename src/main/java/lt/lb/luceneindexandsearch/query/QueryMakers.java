package lt.lb.luceneindexandsearch.query;

import java.util.Objects;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.indexing.IndexingReaderConfig;
import lt.lb.uncheckedutils.func.UncheckedFunction;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 *
 * @author laim0nas100
 */
public class QueryMakers {


    public static UncheckedFunction<String, Query> reversableContentQueryMaker(IndexingReaderConfig indexing, String contentField, String revContentField) {

        Objects.requireNonNull(indexing);
        Objects.requireNonNull(contentField);
        Objects.requireNonNull(revContentField);

        Analyzer analyzer = indexing.getSearchAnalyzer();
        
        return str -> RevFieldWildcardQuery.buildQuery(str, analyzer, contentField, revContentField);
    }
    
    
    public static UncheckedFunction<String, Query> basicContentQueryMaker(IndexingReaderConfig indexing) {
        DocumentFieldsConfig docFields = indexing.getDocumentFieldsConfig();
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(
                docFields.getContentFieldsArray(),
                indexing.getSearchAnalyzer()
        );

        return str -> multiFieldQueryParser.parse(str);
    }

    public static UncheckedFunction<String, Query> basicMainIdQueryMaker(IndexingReaderConfig indexing) {
        DocumentFieldsConfig docFields = indexing.getDocumentFieldsConfig();
        String mainIdFieldName = docFields.getMainIdFieldName();

        return str -> new TermQuery(new Term(mainIdFieldName, str));
    }

}
