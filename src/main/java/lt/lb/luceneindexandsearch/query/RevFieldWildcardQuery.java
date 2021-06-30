package lt.lb.luceneindexandsearch.query;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lt.lb.commons.DLog;
import lt.lb.commons.parsing.Lexer;
import lt.lb.commons.parsing.token.Token;
import lt.lb.commons.parsing.token.match.DefaultMatchedTokenProducer;
import lt.lb.commons.parsing.token.match.MatchedTokens;
import lt.lb.commons.parsing.token.match.TokenMatcher;
import lt.lb.commons.parsing.token.match.TokenMatchers;
import lt.lb.luceneindexandsearch.indexing.content.Premade;
import lt.lb.luceneindexandsearch.indexing.content.SimpleAnalyzer;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.AttributeImpl;

/**
 *
 * @author laim0nas100
 */
public class RevFieldWildcardQuery {

    public static final String OPERATOR_AND = "and";
    public static final String OPERATOR_OR = "or";
    public static final String OPERATOR_NOT = "not";

    public static final String OPERATOR_WILD_QUESTION = "?";
    public static final String OPERATOR_WILD_STAR = "*";
    public static final String OPERATOR_WILD_QUESTION_ESC = "\\?";
    public static final String OPERATOR_WILD_STAR_ESC = "\\*";

    public static final TokenMatcher and = TokenMatchers.exact(OPERATOR_AND);
    public static final TokenMatcher or = TokenMatchers.exact(OPERATOR_OR);
    public static final TokenMatcher not = TokenMatchers.exact(OPERATOR_NOT);

    public static final TokenMatcher wildStar = TokenMatchers.exact(OPERATOR_WILD_STAR).named("wild_star");
    public static final TokenMatcher wildQuestion = TokenMatchers.exact(OPERATOR_WILD_QUESTION).named("wild_question");
    public static final TokenMatcher wildStarEsc = TokenMatchers.exact(OPERATOR_WILD_STAR_ESC).named("wild_star_esc");
    public static final TokenMatcher wildQuestionEsc = TokenMatchers.exact(OPERATOR_WILD_QUESTION_ESC).named("wild_question_escape");
    public static final TokenMatcher literal = TokenMatchers.literalType();

    public static final TokenMatcher concatable = TokenMatchers.or(literal, wildStarEsc, wildQuestionEsc).named("concatable");
    public static final TokenMatcher wildCard = TokenMatchers.or(wildStar, wildQuestion).named("wild_card");
    public static final TokenMatcher wildCard_word = TokenMatchers.concat(wildCard, concatable);
    public static final TokenMatcher word_wildCard = TokenMatchers.concat(concatable, wildCard);
    public static final TokenMatcher wildCard_word_wildcard = TokenMatchers.concat(wildCard, concatable, wildCard);
    public static final TokenMatcher gate = TokenMatchers.or(and, or, not).named("gate");

    static final List<TokenMatcher> asList = Arrays.asList(concatable,
            wildCard_word_wildcard, word_wildCard, wildCard_word, wildCard,
            wildQuestion, wildStar,
            gate, and, or, not
    );

    static final Pattern REPLACE_REPEATING_WILDCARD = Pattern.compile("(\\*+\\?+)|(\\?+\\*+)|(\\*)+");

    public static Query buildQuery(final String term, Analyzer analyzer, String fieldName, String revFieldName) throws IOException, Lexer.StringNotTerminatedException {
        return buildQuery(term, analyzer, fieldName, revFieldName, false);
    }

    public static Query buildQuery(final String term, Analyzer analyzer, String fieldName, String revFieldName, boolean allowAll) throws IOException, Lexer.StringNotTerminatedException {

        String replaced = RegExUtils.replaceAll(term, REPLACE_REPEATING_WILDCARD, "*");

        List<String> terms = tokenizeTerms(replaced, analyzer);

        Lexer lexer = new Lexer();
        lexer.addKeywordBreaking(OPERATOR_WILD_QUESTION, OPERATOR_WILD_QUESTION_ESC, OPERATOR_WILD_STAR, OPERATOR_WILD_STAR_ESC);
        lexer.addKeyword(OPERATOR_AND, OPERATOR_NOT, OPERATOR_OR);
        lexer.setSkipWhitespace(true);
        LinkedList<List<MatchedTokens>> splitMatch = new LinkedList<>();

        for (String t : terms) {
            lexer.resetLines(Arrays.asList(t));
            ArrayList<Token> remainingTokens = lexer.getRemainingTokens();
            DefaultMatchedTokenProducer producer = new DefaultMatchedTokenProducer(remainingTokens.iterator(), asList);
            List<MatchedTokens> matched = Streams.stream(producer).collect(Collectors.toList());
            splitMatch.add(matched);

        }

        if (splitMatch.isEmpty()) {
            return new MatchNoDocsQuery();
        }
        if (splitMatch.size() == 1 && allowAll) {
            List<MatchedTokens> ma = splitMatch.getFirst();
            if (ma.size() == 1) {
                MatchedTokens tokens = ma.get(0);
                if (tokens.contains(wildCard)) {
                    return new BooleanQuery.Builder()
                            .add(new DocValuesFieldExistsQuery(fieldName), BooleanClause.Occur.SHOULD)
                            .add(new DocValuesFieldExistsQuery(revFieldName), BooleanClause.Occur.SHOULD)
                            .build();
                }
            }
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        BooleanClause.Occur nextOccur = BooleanClause.Occur.SHOULD;
        boolean occurChanged = false;
        for (List<MatchedTokens> split : splitMatch) {
            boolean needreverse = false;
            boolean needregular = false;
            boolean simpleTerm = false;
            if (occurChanged) {
                occurChanged = false;
            } else {
                nextOccur = BooleanClause.Occur.SHOULD;
            }

            LinkedList<Token> q = new LinkedList<>();
            LinkedList<Token> rev = new LinkedList<>();
            for (MatchedTokens ma : split) {

                if (ma.contains(wildCard_word_wildcard)) { // main case

                    q.addAll(ma.getTokens(0, 1, 2));
                    rev.addAll(0, ma.getTokens(2, 1, 0));
                    needreverse = true;
                    needregular = true;
                } else if (ma.contains(word_wildCard)) {
                    q.addAll(ma.getTokens(0, 1));
                    rev.addAll(0, ma.getTokens(1, 0));
                    needregular = true;
                } else if (ma.contains(wildCard_word)) {
                    q.addAll(ma.getTokens(0, 1));
                    rev.addAll(0, ma.getTokens(1, 0));
                    needreverse = true;
                } else if (ma.contains(concatable)) {
                    q.addAll(ma.getTokens(0));
                    rev.addFirst(ma.getToken(0));
                    simpleTerm = true;
                } else if (ma.contains(gate)) {

                    occurChanged = true;
                    if (ma.contains(and)) {
                        nextOccur = BooleanClause.Occur.MUST;
                    } else if (ma.contains(or)) {
                        nextOccur = BooleanClause.Occur.SHOULD;
                    } else if (ma.contains(not)) {
                        nextOccur = BooleanClause.Occur.MUST_NOT;

                    }
                } else if (ma.contains(wildCard)) {
                    q.addAll(ma.getTokens(0));
                    rev.addFirst(ma.getToken(0));
                } else {
                    //irrelevant
                }
            }

            if (needreverse || needregular || simpleTerm) {
                List<Query> querys = new ArrayList<>();
                if (needregular) {
                    while (!q.isEmpty() && wildCard.test(q.getFirst())) {
                        // need to remove first wild card
                        q.removeFirst();
                    }
                    String query = q.stream().map(m -> m.value).collect(Collectors.joining());
                    querys.add(new WildcardQuery(new Term(fieldName, query)));

                }
                if (needreverse) {
                    while (!rev.isEmpty() && wildCard.test(rev.getFirst())) {
                        // need to remove first wild card
                        rev.removeFirst();
                    }
                    String revQeury = rev.stream().map(m -> m.value).map(StringUtils::reverse).collect(Collectors.joining());
                    querys.add(new WildcardQuery(new Term(revFieldName, revQeury)));

                }
                if (simpleTerm && !(needregular || needreverse)) {
                    String query = q.stream().map(m -> m.value).collect(Collectors.joining());
                    querys.add(new TermQuery(new Term(fieldName, query)));
                }
                if (querys.size() > 0) {
                    if (querys.size() > 1) {
                        BooleanQuery.Builder inner = new BooleanQuery.Builder();
                        for (Query query : querys) {
                            inner.add(query, BooleanClause.Occur.SHOULD);
                        }
                        builder.add(inner.build(), nextOccur);
                    } else {
                        builder.add(querys.get(0), nextOccur);
                    }
                }
            } else {
                // no query
            }

        }
        return builder.build();
    }

    public static List<String> tokenizeTerms(String tokenize, Analyzer analyzer) throws IOException {
        ArrayList<String> terms = new ArrayList<>();
        try (TokenStream tokenStream = analyzer.tokenStream("anyField", tokenize)) {
            tokenStream.reset();

            while (true) {
                boolean hasToken = tokenStream.incrementToken();
                if (!hasToken) {
                    break;
                }
                boolean hasChar = tokenStream.hasAttribute(CharTermAttribute.class);
                if (!hasChar) {
                    continue;
                }
                CharTermAttribute attribute = tokenStream.getAttribute(CharTermAttribute.class);
                String term = new String(attribute.buffer(), 0, attribute.length());
                terms.add(term);

            }
        }
        return terms;

    }

    public static void main(String[] args) throws Exception {
        DLog.main().async = false;
        SimpleAnalyzer defaultAnalyzer = Premade.defaultSearchAnalyzer();
        
        

        String term = "*hell?o?? *help?me?jesus? " + " NOT " + " **something else?* regular";
//        String term = "*";

        TokenStream tokenStream = defaultAnalyzer.tokenStream(OPERATOR_AND, term);
        tokenStream.reset();
        while(true){
           
            boolean inc = tokenStream.incrementToken();
           
             CharTermAttribute attribute = tokenStream.getAttribute(CharTermAttribute.class);
            DLog.print(attribute);
             if(!inc){
                break;
            }
        }
        tokenStream.close();
        Query query = buildQuery(term, defaultAnalyzer, "field", "revField");

        DLog.print(query);
        DLog.close();

    }
}
