package lt.lb.luceneindexandsearch.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import lt.lb.lucenejpa.SyncDirectory;

/**
 *
 * @author laim0nas100
 */
public interface GrowingMultiDirectory<P> extends Resolver<P, SyncDirectory> {

    Map<P, SyncDirectory> getDirectoryMap() throws IOException;
    
    default Collection<SyncDirectory> getDirectories() throws IOException {
        return getDirectoryMap().values();
    }
}
