package lt.lb.luceneindexandsearch.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lt.lb.commons.DLog;
import lt.lb.configurablelexer.anymatch.PosMatch;
import lt.lb.configurablelexer.anymatch.PosMatched;
import lt.lb.configurablelexer.anymatch.SimpleStringPosMatcherCombinator;
import lt.lb.configurablelexer.anymatch.impl.Matchers;
import lt.lb.configurablelexer.lexer.SimpleLexer;
import lt.lb.configurablelexer.lexer.matchers.KeywordMatcher;
import lt.lb.configurablelexer.lexer.matchers.StringMatcher;
import lt.lb.configurablelexer.parse.DefaultMatchedTokenProducer;
import lt.lb.configurablelexer.parse.MatchedTokens;
import lt.lb.configurablelexer.parse.TokenMatcher;
import lt.lb.configurablelexer.parse.TokenMatchers;
import lt.lb.configurablelexer.token.ConfToken;
import lt.lb.configurablelexer.token.DefaultConfTokenizer;
import lt.lb.configurablelexer.token.base.KeywordToken;
import lt.lb.configurablelexer.token.base.LiteralToken;
import lt.lb.configurablelexer.token.base.StringToken;
import lt.lb.luceneindexandsearch.indexing.content.Premade;
import lt.lb.luceneindexandsearch.indexing.content.SimpleAnalyzer;
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

    public static final String OPERATOR_AND = "and";
    public static final String OPERATOR_OR = "or";
    public static final String OPERATOR_NOT = "not";
    

    public static final String OPERATOR_WILD_QUESTION = "?";
    public static final String OPERATOR_WILD_STAR = "*";
    public static final String OPERATOR_WILD_QUESTION_ESC = "\\?";
    public static final String OPERATOR_WILD_STAR_ESC = "\\*";

    public static final Matchers<ConfToken,String> M = new Matchers<ConfToken,String>().setDefaultName("no name");
    
    public static final PosMatch<ConfToken,String> and = exact(OPERATOR_AND);
    public static final PosMatch<ConfToken,String> or = exact(OPERATOR_OR);
    public static final PosMatch<ConfToken,String> not = exact(OPERATOR_NOT);

    
    public static final PosMatch<ConfToken,String> wildStar = exact(OPERATOR_WILD_STAR);
    public static final PosMatch<ConfToken,String> wildQuestion = exact(OPERATOR_WILD_QUESTION);
    public static final PosMatch<ConfToken,String> wildStarEsc = exact(OPERATOR_WILD_STAR_ESC);
    public static final PosMatch<ConfToken,String> wildQuestionEsc = exact(OPERATOR_WILD_QUESTION_ESC);
    public static final PosMatch<ConfToken,String> literal = M.makeNew("literal").ofType(LiteralToken.class);

    public static final PosMatch<ConfToken,String> concatable = M.makeNew("concatable").or(literal, wildStarEsc, wildQuestionEsc);
    public static final PosMatch<ConfToken,String> wildCard = M.makeNew("wild_card").or(wildStar, wildQuestion);
    public static final PosMatch<ConfToken,String> wildCard_word = M.makeNew("wildCard_word").concat(wildCard, concatable);
    public static final PosMatch<ConfToken,String> word_wildCard = M.makeNew("word_wildCard").concat(concatable, wildCard);
    public static final PosMatch<ConfToken,String> wildCard_word_wildcard = M.makeNew("wildCard_word_wildcard").concat(wildCard, concatable, wildCard);
    public static final PosMatch<ConfToken,String> gate = M.makeNew("gate").or(and, or, not);

    static final List<PosMatch<ConfToken,String>> asList = Arrays.asList(concatable,
            wildCard_word_wildcard, word_wildCard, wildCard_word, wildCard,
            wildQuestion, wildStar,
            gate, and, or, not
    );
    
    private static PosMatch<ConfToken,String> exact(String str){
        return M.makeNew(str).isWhen(c->StringUtils.equals(c.getValue(), str));
    }

    static final Pattern REPLACE_REPEATING_WILDCARD = Pattern.compile("(\\*+\\?+)|(\\?+\\*+)|(\\*)+");

    public static Query buildQuery(final String term, Analyzer analyzer, String fieldName, String revFieldName) throws Exception {
        return buildQuery(term, analyzer, fieldName, revFieldName, false, BooleanClause.Occur.MUST); // default AND
    }

    public static Query buildQuery(final String term, Analyzer analyzer, String fieldName, String revFieldName, boolean allowAll, BooleanClause.Occur defaultOccur) throws Exception {

        String replaced = RegExUtils.replaceAll(term, REPLACE_REPEATING_WILDCARD, "*");

        List<String> terms = tokenizeTerms(replaced, analyzer);
        DefaultConfTokenizer tokenizer = new DefaultConfTokenizer();
        SimpleLexer simpleLexer = new SimpleLexer(tokenizer) {
            @Override
            public ConfToken makeLexeme(int from, int to, StringMatcher.MatcherMatch matcher, String unbrokenString) throws Exception {
                String val = unbrokenString.substring(from, to);
                if (matcher.match instanceof KeywordMatcher) {
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
        LinkedList<List<PosMatched<ConfToken,String>>> splitMatch = new LinkedList<>();

        for (String t : terms) {
            tokenizer.reset(t);
            tokenizer.toSimplifiedIterator().iterator();
            List<PosMatched<ConfToken,String>> produceItems = new SimpleStringPosMatcherCombinator<>(tokenizer.toSimplifiedIterator().iterator(),asList).produceItems();
            splitMatch.add(produceItems);

        }

        if (splitMatch.isEmpty()) {
            return new MatchNoDocsQuery();
        }
        if (splitMatch.size() == 1 && allowAll) {
            List<PosMatched<ConfToken,String>> ma = splitMatch.getFirst();
            if (ma.size() == 1) {
                PosMatched<ConfToken,String> tokens = ma.get(0);
                if (tokens.contains(wildCard.getName())) {
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
        for (List<PosMatched<ConfToken,String>> split : splitMatch) {
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
            for (PosMatched<ConfToken,String> ma : split) {

                if (ma.contains(wildCard_word_wildcard.getName())) { // main case

                    q.addAll(ma.getItems(0, 1, 2));
                    rev.addAll(0, ma.getItems(2, 1, 0));
                    needreverse = true;
                    needregular = true;
                } else if (ma.contains(word_wildCard.getName())) {
                    q.addAll(ma.getItems(0, 1));
                    rev.addAll(0, ma.getItems(1, 0));
                    needregular = true;
                } else if (ma.contains(wildCard_word.getName())) {
                    q.addAll(ma.getItems(0, 1));
                    rev.addAll(0, ma.getItems(1, 0));
                    needreverse = true;
                } else if (ma.contains(concatable.getName())) {
                    q.addAll(ma.getItems(0));
                    rev.addFirst(ma.getItem(0));
                    simpleTerm = true;
                } else if (ma.contains(gate.getName())) {

                    occurChanged = true;
                    if (ma.contains(and.getName())) {
                        nextOccur = BooleanClause.Occur.MUST;
                    } else if (ma.contains(or.getName())) {
                        nextOccur = BooleanClause.Occur.SHOULD;
                    } else if (ma.contains(not.getName())) {
                        nextOccur = BooleanClause.Occur.MUST_NOT;

                    }
                } else if (ma.contains(wildCard.getName())) {
                    q.addAll(ma.getItems(0));
                    rev.addFirst(ma.getItem(0));
                } else {
                    //irrelevant
                }
            }

            if (needreverse || needregular || simpleTerm) {
                List<Query> querys = new ArrayList<>();
                if (needregular) {
                    while (!q.isEmpty() && wildCard.matches(0,q.getFirst())) {
                        // need to remove first wild card
                        q.removeFirst();
                    }
                    String query = q.stream().map(m -> m.getValue()).collect(Collectors.joining());
                    querys.add(new WildcardQuery(new Term(fieldName, query)));

                }
                if (needreverse) {
                    while (!rev.isEmpty() && wildCard.matches(0,rev.getFirst())) {
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

    public static String tokenizeTermsToString(String tokenize, Analyzer analyzer) throws IOException {
        return tokenizeTerms(tokenize, analyzer).stream().collect(Collectors.joining(" "));
    }

    public static void main(String[] args) throws Exception {
        DLog.main().async = false;
        SimpleAnalyzer defaultAnalyzer = Premade.defaultSearchAnalyzer();

        String term = "*hell?o?? *help?me?jesus? " + " NOT " + " **something else?* regular";
//        String term = "*help?me?jesus? ";

        TokenStream tokenStream = defaultAnalyzer.tokenStream(OPERATOR_AND, term);
        tokenStream.reset();
        while (true) {

            boolean inc = tokenStream.incrementToken();

            CharTermAttribute attribute = tokenStream.getAttribute(CharTermAttribute.class);
            DLog.print(attribute);
            if (!inc) {
                break;
            }
        }
        tokenStream.close();
        Query query = buildQuery(term, defaultAnalyzer, "field", "revField");

        DLog.print(query);
        DLog.close();

    }
}
