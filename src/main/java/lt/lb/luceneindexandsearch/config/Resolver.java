package lt.lb.luceneindexandsearch.config;

import java.io.IOException;
import java.util.function.Function;
import lt.lb.uncheckedutils.Checked;

/**
 *
 * @author laim0nas100
 */
public interface Resolver<Property, Resolvable> extends Function<Property, Resolvable> {

    public Resolvable resolve(Property t) throws IOException;

    @Override
    public default Resolvable apply(Property t) {
        return Checked.uncheckedCall(() -> resolve(t));
    }

}
