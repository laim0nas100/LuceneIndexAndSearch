package lt.lb.luceneindexandsearch.config.lazyimpl;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lt.lb.commons.misc.compare.Compare;

/**
 *
 * @author laim0nas100
 */
public class LuceneCachedMapImpl<ID, D extends Comparable<D>> implements LuceneCachedMap<ID, D> {

    protected volatile Map<ID, D> map;
    protected volatile ID maxChangedID;
    protected volatile D maxChanged;
    
    public  static <ID, D extends Comparable<D>> LuceneCachedMapImpl<ID,D> empty(){
        ImmutableMap<ID, D> map = ImmutableMap.of();
        return new LuceneCachedMapImpl<>(map);
    }

    public LuceneCachedMapImpl(Map<ID, D> map) {
        this.map = Objects.requireNonNull(map);
        recalculate();
    }

    @Override
    public void recalculate() {
        List<Map.Entry<ID, D>> entries = new ArrayList<>(map.entrySet());
        for (Map.Entry<ID, D> en : entries) {
            if (Compare.compareNullLower(maxChanged, Compare.CompareOperator.LESS, en.getValue())) {
                maxChanged = en.getValue();
                maxChangedID = en.getKey();
            }
        }

    }

    @Override
    public Optional<D> getLastChanged() {
        return Optional.ofNullable(maxChanged);
    }

    @Override
    public Optional<ID> getLastChangedID() {
        return Optional.ofNullable(maxChangedID);
    }

    @Override
    public Map<ID, D> getMap() {
        return map;
    }

}
