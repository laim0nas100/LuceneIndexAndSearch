package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.util.Map;
import java.util.Optional;

/**
 *
 * @author laim0nas100
 */
public interface LuceneCachedMap<ID, D extends Comparable<D>> {

    Optional<D> getLastChanged();

    Optional<ID> getLastChangedID();

    void recalculate();

    Map<ID, D> getMap();
    
    void updateWith(Map<ID,D> map);

}
