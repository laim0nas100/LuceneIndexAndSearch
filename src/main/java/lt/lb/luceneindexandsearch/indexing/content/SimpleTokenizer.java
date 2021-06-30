package lt.lb.luceneindexandsearch.indexing.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.util.CharTokenizer;

/**
 *
 * @author laim0nas100
 */
public class SimpleTokenizer extends CharTokenizer {

    private Set<Integer> allowedCodepoints = new HashSet<>();
    private ArrayList<Predicate<Integer>> predicates = new ArrayList<>();

    public void addAllowedChar(char ch) {
        allowedCodepoints.add((int) ch);
    }

    public void addAllowedChars(char... ch) {
        for (int i : ch) {
            allowedCodepoints.add(i);
        }
    }

    public void addAllowedChar(int ch) {
        allowedCodepoints.add(ch);
    }

    public void addAllowedChars(int... ch) {
        for (int i : ch) {
            allowedCodepoints.add(i);
        }

    }

    public void addAllowedPredicate(Predicate<Integer> pred) {
        Objects.requireNonNull(pred);
        predicates.add(pred);
    }

    @Override
    protected boolean isTokenChar(int i) {
        for (Predicate<Integer> pred : predicates) {
            if (pred.test(i)) {
                return true;
            }
        }
        return allowedCodepoints.contains(i);
    }

}
