package test;

import lt.lb.commons.DLog;
import lt.lb.luceneindexandsearch.indexing.content.Premade;
import lt.lb.luceneindexandsearch.indexing.content.SimpleAnalyzer;
import static lt.lb.luceneindexandsearch.query.RevFieldWildcardQuery.OPERATOR_AND;
import static lt.lb.luceneindexandsearch.query.RevFieldWildcardQuery.buildQuery;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.Query;

/**
 *
 * @author laim0nas100
 */
public class TestRevFieldWildcardQuery {
    public static void main(String[] args) throws Exception {
        DLog.main().async = true;
        SimpleAnalyzer defaultAnalyzer = Premade.defaultSearchAnalyzer();

        String term = "*hell?o?? *help?me?jesus? " + " NOT " + " **something else?* regular";
//        String term = "*13225456 ";

//        String term = " ";
        
//        for (int i = 0; i < 10; i++) {
//            term = term + " " + term;
//        }

        TokenStream tokenStream = defaultAnalyzer.tokenStream(OPERATOR_AND, term);
        tokenStream.reset();
        while (true) {

            boolean inc = tokenStream.incrementToken();

            CharTermAttribute attribute = tokenStream.getAttribute(CharTermAttribute.class);
            DLog.print(attribute.toString());
            if (!inc) {
                break;
            }
        }
        tokenStream.close();
        DLog.print("Build query");
        Query query = buildQuery(term, defaultAnalyzer, "field", "revField");
        DLog.print("After build");
        DLog.print(query);
        DLog.close();

    }
}
