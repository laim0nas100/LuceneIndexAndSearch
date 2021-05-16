package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.util.Map;
import java.util.Optional;

/**
 *
 * @author laim0nas100
 */
public interface LuceneCachedMap<ID, D extends Comparable<D>> {

    public Optional<D> getLastChanged();

    public Optional<ID> getLastChangedID();

    public void recalculate();

    public Map<ID, D> getMap();

}
