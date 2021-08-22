package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lt.lb.luceneindexandsearch.config.GrowingMultiDirectory;
import lt.lb.luceneindexandsearch.config.Resolver;
import lt.lb.lucenejpa.KindConfig;
import lt.lb.lucenejpa.SyncDirectory;

/**
 *
 * @author laim0nas100
 */
public class LazyGrowingMultiDirectory<P> implements GrowingMultiDirectory<P> {

    protected Resolver<P, SyncDirectory> resolver;
    protected ConcurrentHashMap<P, SyncDirectory> map = new ConcurrentHashMap<>();
    protected KindConfig kindConfig;

    public LazyGrowingMultiDirectory(Resolver<P, SyncDirectory> resolver, KindConfig kindConfig) {
        this.resolver = Objects.requireNonNull(resolver);
        this.kindConfig = Objects.requireNonNull(kindConfig);
    }

    @Override
    public Map<P, SyncDirectory> getDirectoryMap() throws IOException {
        return map;
    }

    @Override
    public SyncDirectory resolve(P t) throws IOException {
        if (map.containsKey(t)) {
            return map.get(t);
        }
        SyncDirectory apply = resolver.apply(t);
        map.put(t, apply);
        return apply;

    }

    public Resolver<P, SyncDirectory> getResolver() {
        return resolver;
    }

    public KindConfig getKind() {
        return kindConfig;
    }

}
