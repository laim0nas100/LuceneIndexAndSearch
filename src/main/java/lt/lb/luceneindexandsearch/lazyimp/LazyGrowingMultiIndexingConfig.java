package lt.lb.luceneindexandsearch.lazyimp;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lt.lb.luceneindexandsearch.config.GrowingMultiDirectory;
import lt.lb.luceneindexandsearch.config.GrowingMultiIndexingConfig;
import lt.lb.luceneindexandsearch.config.IndexingConfig;
import lt.lb.luceneindexandsearch.config.Resolver;
import lt.lb.lucenejpa.SyncDirectory;


public class LazyGrowingMultiIndexingConfig<P> implements GrowingMultiIndexingConfig<P> {

    protected GrowingMultiDirectory<P> multiDirectories;
    protected Resolver<SyncDirectory, IndexingConfig> directoryResolver;
    protected ConcurrentHashMap<P, IndexingConfig> map = new ConcurrentHashMap<>();

    @Override
    public IndexingConfig resolve(P t) throws IOException {
        if(map.containsKey(t)){
            return map.get(t);
        }
        SyncDirectory directory = multiDirectories.resolve(t);
        IndexingConfig apply = directoryResolver.apply(directory);
        map.put(t, apply);
        return apply;

    }

    public Resolver<SyncDirectory, IndexingConfig> getDirectoryResolver() {
        return directoryResolver;
    }

    public void setDirectoryResolver(Resolver<SyncDirectory, IndexingConfig> directoryResolver) {
        this.directoryResolver = directoryResolver;
    }
    
    @Override
    public GrowingMultiDirectory<P> getMultiDirectories() {
        return multiDirectories;
    }

    public void setMultiDirectories(GrowingMultiDirectory<P> multiDirectories) {
        this.multiDirectories = multiDirectories;
    }
    
    @Override
    public Map<P, IndexingConfig> getIndexingConfigMap() {
        return map;
    }

    
}
