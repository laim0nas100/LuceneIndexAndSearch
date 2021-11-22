package lt.lb.luceneindexandsearch.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lt.lb.configurablelexer.anymatch.PosMatched;
import lt.lb.configurablelexer.anymatch.impl.ConfMatchers;
import lt.lb.configurablelexer.anymatch.impl.ConfMatchers.PM;
import lt.lb.configurablelexer.anymatch.impl.SimplePosMatcherCombinator;
import lt.lb.configurablelexer.lexer.SimpleLexer;
import lt.lb.configurablelexer.lexer.SimpleLexerOptimized;
import lt.lb.configurablelexer.lexer.matchers.KeywordMatcher;
import lt.lb.configurablelexer.lexer.matchers.StringMatcher;
import lt.lb.configurablelexer.token.ConfToken;
import lt.lb.configurablelexer.token.DefaultConfTokenizer;
import lt.lb.configurablelexer.token.base.KeywordToken;
import lt.lb.configurablelexer.token.base.LiteralToken;
import lt.lb.configurablelexer.token.base.StringToken;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 *
 * @author laim0nas100
 */
public class RevFieldWildcardQuery {

    public static final int TERMS_HARD_LIMIT = 128;

    public static final String OPERATOR_AND = "and";
    public static final String OPERATOR_OR = "or";
    public static final String OPERATOR_NOT = "not";

    public static final String OPERATOR_WILD_QUESTION = "?";
    public static final String OPERATOR_WILD_STAR = "*";
    public static final String OPERATOR_WILD_QUESTION_ESC = "\\?";
    public static final String OPERATOR_WILD_STAR_ESC = "\\*";

    public static final ConfMatchers M = new ConfMatchers();

    public static final PM and = exact(OPERATOR_AND);
    public static final PM or = exact(OPERATOR_OR);
    public static final PM not = exact(OPERATOR_NOT);

    public static final PM wildStar = exact(OPERATOR_WILD_STAR);
    public static final PM wildQuestion = exact(OPERATOR_WILD_QUESTION);
    public static final PM wildStarEsc = exact(OPERATOR_WILD_STAR_ESC);
    public static final PM wildQuestionEsc = exact(OPERATOR_WILD_QUESTION_ESC);
    public static final PM literal = M.makeNew("literal").ofType(LiteralToken.class);

    public static final PM concatable = M.makeNew("concatable").or(literal, wildStarEsc, wildQuestionEsc);
    public static final PM wildCard = M.makeNew("wild_card").or(wildStar, wildQuestion);
    public static final PM wildCard_word = M.makeNew("wildCard_word").concat(wildCard, concatable);
    public static final PM word_wildCard = M.makeNew("word_wildCard").concat(concatable, wildCard);
    public static final PM wildCard_word_wildcard = M.makeNew("wildCard_word_wildcard").concat(wildCard, concatable, wildCard);
    public static final PM gate = M.makeNew("gate").or(and, or, not);

    static final List<PM> asList = makeList();

    static List<PM> makeList() {
        List<PM> list = Arrays.asList(concatable,
                wildCard_word_wildcard, word_wildCard, wildCard_word, wildCard,
                wildQuestion, wildStar,
                gate, and, or, not
        );
        list.sort(SimplePosMatcherCombinator.cmpMatchers);
        return list;
    }

    private static PM exact(String str) {
        return M.makeNew(str).isWhen(c -> StringUtils.equals(c.getValue(), str));
    }

    static final Pattern REPLACE_REPEATING_WILDCARD = Pattern.compile("(\\*+\\?+)|(\\?+\\*+)|(\\*)+");

    public static Query buildQuery(final String query, Analyzer analyzer, String fieldName, String revFieldName) throws Exception {
        return buildQuery(query, analyzer, fieldName, revFieldName, false, BooleanClause.Occur.MUST); // default AND
    }

    public static DefaultConfTokenizer<ConfToken> buildTokenizer() {
        DefaultConfTokenizer tokenizer = new DefaultConfTokenizer();
        SimpleLexer simpleLexer = new SimpleLexerOptimized(tokenizer) {
            @Override
            public ConfToken makeLexeme(int from, int to, StringMatcher.MatcherMatch matcher, String unbrokenString) throws Exception {
                String val = unbrokenString.substring(from, to);
                if (matcher.matcher instanceof KeywordMatcher) {
                    return new KeywordToken(val);
                }
                return new StringToken(val);
            }

            @Override
            public ConfToken makeLiteral(int from, int to, String unbrokenString) throws Exception {
                String val = unbrokenString.substring(from, to);
                return new LiteralToken(val);
            }
        };

        tokenizer.getConfCallbacks().nest(f -> simpleLexer);

        simpleLexer.addMatcher(new KeywordMatcher(OPERATOR_WILD_QUESTION, true));
        simpleLexer.addMatcher(new KeywordMatcher(OPERATOR_WILD_QUESTION_ESC, true));
        simpleLexer.addMatcher(new KeywordMatcher(OPERATOR_WILD_STAR, true));
        simpleLexer.addMatcher(new KeywordMatcher(OPERATOR_WILD_STAR_ESC, true));
        simpleLexer.addMatcher(new KeywordMatcher(OPERATOR_AND, false));
        simpleLexer.addMatcher(new KeywordMatcher(OPERATOR_NOT, false));
        simpleLexer.addMatcher(new KeywordMatcher(OPERATOR_OR, false));

        return tokenizer;
    }

    public static Query buildQuery(final String search, Analyzer analyzer, String fieldName, String revFieldName, boolean allowAll, BooleanClause.Occur defaultOccur) throws Exception {

        String replaced = RegExUtils.replaceAll(search, REPLACE_REPEATING_WILDCARD, "*");
        List<String> terms = tokenizeTerms(replaced, analyzer);
        LinkedList<List<PosMatched<ConfToken, String>>> splitMatch = new LinkedList<>();
        DefaultConfTokenizer<ConfToken> tokenizer = buildTokenizer();

        for (String t : terms) {
            tokenizer.reset(t);
            splitMatch.add(SimplePosMatcherCombinator.tryMatchAll(false, tokenizer.toSimplifiedIterator().iterator(), asList));
        }

        if (splitMatch.isEmpty()) {
            return new MatchNoDocsQuery();
        }
        if (splitMatch.size() == 1 && allowAll) {
            List<PosMatched<ConfToken, String>> ma = splitMatch.getFirst();
            if (ma.size() == 1) {
                PosMatched<ConfToken, String> tokens = ma.get(0);
                if (tokens.containsMatcher(wildCard.getName())) {
                    return new BooleanQuery.Builder()
                            .add(new DocValuesFieldExistsQuery(fieldName), BooleanClause.Occur.SHOULD)
                            .add(new DocValuesFieldExistsQuery(revFieldName), BooleanClause.Occur.SHOULD)
                            .build();
                }
            }
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        BooleanClause.Occur nextOccur = defaultOccur;
        boolean occurChanged = false;
        for (List<PosMatched<ConfToken, String>> split : splitMatch) {
            boolean needreverse = false;
            boolean needregular = false;
            boolean simpleTerm = false;
            if (occurChanged) {
                occurChanged = false;
            } else {
                nextOccur = defaultOccur;
            }

            LinkedList<ConfToken> q = new LinkedList<>();
            LinkedList<ConfToken> rev = new LinkedList<>();
            for (PosMatched<ConfToken, String> ma : split) {

                if (ma.containsMatcher(wildCard_word_wildcard.getName())) { // main case

                    q.addAll(ma.getItems(0, 1, 2));
                    rev.addAll(0, ma.getItems(2, 1, 0));
                    needreverse = true;
                    needregular = true;
                } else if (ma.containsMatcher(word_wildCard.getName())) {
                    q.addAll(ma.getItems(0, 1));
                    rev.addAll(0, ma.getItems(1, 0));
                    needregular = true;
                } else if (ma.containsMatcher(wildCard_word.getName())) {
                    q.addAll(ma.getItems(0, 1));
                    rev.addAll(0, ma.getItems(1, 0));
                    needreverse = true;
                } else if (ma.containsMatcher(concatable.getName())) {
                    q.addAll(ma.getItems(0));
                    rev.addFirst(ma.getItem(0));
                    simpleTerm = true;
                } else if (ma.containsMatcher(gate.getName())) {

                    occurChanged = true;
                    if (ma.containsMatcher(and.getName())) {
                        nextOccur = BooleanClause.Occur.MUST;
                    } else if (ma.containsMatcher(or.getName())) {
                        nextOccur = BooleanClause.Occur.SHOULD;
                    } else if (ma.containsMatcher(not.getName())) {
                        nextOccur = BooleanClause.Occur.MUST_NOT;

                    }
                } else if (ma.containsMatcher(wildCard.getName())) {
                    q.addAll(ma.getItems(0));
                    rev.addFirst(ma.getItem(0));
                } else {
                    //irrelevant
                }
            }

            List<Query> querys = new ArrayList<>();
            if (needregular) {
                while (!q.isEmpty() && wildCard.matches(0, q.getFirst())) {
                    // need to remove first wild card
                    q.removeFirst();
                }
                String query = q.stream().map(m -> m.getValue()).collect(Collectors.joining());
                querys.add(new WildcardQuery(new Term(fieldName, query)));

            }
            if (needreverse) {
                while (!rev.isEmpty() && wildCard.matches(0, rev.getFirst())) {
                    // need to remove first wild card
                    rev.removeFirst();
                }
                String revQeury = rev.stream().map(m -> m.getValue()).map(StringUtils::reverse).collect(Collectors.joining());
                querys.add(new WildcardQuery(new Term(revFieldName, revQeury)));

            }
            if (simpleTerm && !(needregular || needreverse)) {
                String query = q.stream().map(m -> m.getValue()).collect(Collectors.joining());
                querys.add(new TermQuery(new Term(fieldName, query)));
            }
            if (querys.size() > 0) {
                if (querys.size() > 1) {
                    BooleanQuery.Builder inner = new BooleanQuery.Builder();
                    for (Query query : querys) {
                        inner.add(query, BooleanClause.Occur.SHOULD); // duplicating querys
                    }
                    builder.add(inner.build(), nextOccur);
                } else {
                    builder.add(querys.get(0), nextOccur);
                }
            }

        }
        return builder.build();
    }

    public static List<String> tokenizeTerms(String tokenize, Analyzer analyzer) throws IOException {
        ArrayList<String> terms = new ArrayList<>();
        try (TokenStream tokenStream = analyzer.tokenStream("anyField", tokenize)) {
            tokenStream.reset();

            int count = 0;
            while (count < TERMS_HARD_LIMIT) {

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
                count++;

            }
        }
        return terms;

    }

    public static String tokenizeTermsToString(String tokenize, Analyzer analyzer) throws IOException {
        return tokenizeTerms(tokenize, analyzer).stream().collect(Collectors.joining(" "));
    }

}
