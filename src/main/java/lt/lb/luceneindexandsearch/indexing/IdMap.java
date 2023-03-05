package lt.lb.luceneindexandsearch.indexing;

import java.util.Map;
import java.util.Objects;

/**
 *
 * @author laim0nas100
 */
public class IdMap<ID> {

    public final Map<String, String> map;
    public final ID id;

    public IdMap(ID id, Map<String, String> map) {
        this.map = Objects.requireNonNull(map);
        this.id = Objects.requireNonNull(id);
    }

}
