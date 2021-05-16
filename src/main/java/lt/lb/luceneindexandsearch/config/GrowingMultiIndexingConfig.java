package lt.lb.luceneindexandsearch.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author laim0nas100
 * @param <P>
 */
public interface GrowingMultiIndexingConfig<P> extends Resolver<P, IndexingConfig> {

    public GrowingMultiDirectory<P> getMultiDirectories();

    Map<P, IndexingConfig> getIndexingConfigMap();

    default Collection<IndexingConfig> getIndexingConfigs() {
        return getIndexingConfigMap().values();
    }

    default void initMissingConfigs() throws IOException {
        Set<P> keySet = getMultiDirectories().getDirectoryMap().keySet();
        for (P key : keySet) {
            this.resolve(key);
        }
    }
    
}
