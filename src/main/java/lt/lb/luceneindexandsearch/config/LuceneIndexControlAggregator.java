package lt.lb.luceneindexandsearch.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author laim0nas100
 */
public interface LuceneIndexControlAggregator {

    public void setupMaintenance();
    
    public void cancelMaintenance();

    public Map<String, LuceneIndexControl> getLuceneIndexControlMap();

    public default List<LuceneIndexControl> getLuceneIndexControls() {
        return new ArrayList<>(getLuceneIndexControlMap().values());
    }

}
