package lt.lb.luceneindexandsearch.indexing.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

/**
 *
 * @author laim0nas100
 */
public class SimpleAnalyzer extends Analyzer {

    protected Supplier<? extends Tokenizer> tokenizerFactory;
    protected List<BiFunction<String, TokenStream, TokenStream>> filters = new ArrayList<>();

    public SimpleAnalyzer(Supplier<? extends Tokenizer> tokenizerFactory) {
        this.tokenizerFactory = Objects.requireNonNull(tokenizerFactory);
    }

    public SimpleAnalyzer(Supplier<? extends Tokenizer> tokenizerFactory, ReuseStrategy reuseStrategy) {
        super(reuseStrategy);
        this.tokenizerFactory = Objects.requireNonNull(tokenizerFactory);
    }

    public void add(Function<TokenStream, TokenStream> filter) {
        Objects.requireNonNull(filter);
        add((field, stream) -> filter.apply(stream));
    }

    public void add(BiFunction<String, TokenStream, TokenStream> filter) {
        Objects.requireNonNull(filter);
        filters.add(filter);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer src = tokenizerFactory.get();
        TokenStream filtered = src;
        for (BiFunction<String, TokenStream, TokenStream> filter : filters) {
            filtered = filter.apply(fieldName, filtered);
        }
        return new TokenStreamComponents(src, filtered);
    }

}
