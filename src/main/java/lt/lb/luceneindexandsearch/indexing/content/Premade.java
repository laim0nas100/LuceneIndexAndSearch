package lt.lb.luceneindexandsearch.indexing.content;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

/**
 *
 * @author laim0nas100
 */
public class Premade {

    public static SimpleTokenizer defaultSearchTokenizer() {
        SimpleTokenizer tokenizer = new SimpleTokenizer();
        tokenizer.addAllowedChars('*', '?', '-', '/', '\\');
        tokenizer.addAllowedPredicate(Character::isLetterOrDigit);
        return tokenizer;
    }

    public static SimpleTokenizer defaultIndexTokenizer() {
        SimpleTokenizer tokenizer = new SimpleTokenizer();
        tokenizer.addAllowedChars('-', '/', '\\');
        tokenizer.addAllowedPredicate(Character::isLetterOrDigit);
        return tokenizer;
    }

    public static SimpleAnalyzer defaultIndexAnalyzer() {
        SimpleAnalyzer analyzer = new SimpleAnalyzer(() -> defaultIndexTokenizer());
        analyzer.add(to -> {
            return new LowerCaseFilter(to);
        });
        analyzer.add(to -> {
            return new ASCIIFoldingFilter(to, false);
        });

        return analyzer;
    }

    public static SimpleAnalyzer defaultSearchAnalyzer() {
        SimpleAnalyzer analyzer = new SimpleAnalyzer(() -> defaultSearchTokenizer());
        analyzer.add(to -> {
            return new LowerCaseFilter(to);
        });
        analyzer.add(to -> {
            return new ASCIIFoldingFilter(to, false);
        });

        return analyzer;
    }
}
