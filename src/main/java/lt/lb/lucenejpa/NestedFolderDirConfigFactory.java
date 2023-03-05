package lt.lb.lucenejpa;

import lt.lb.luceneindexandsearch.splitting.KindConfig;
import lt.lb.luceneindexandsearch.splitting.DirConfig;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lt.lb.uncheckedutils.concurrent.CheckedExecutor;
import org.apache.lucene.util.IOUtils;

/**
 *
 * @author laim0nas100
 */
public abstract class NestedFolderDirConfigFactory<T extends DirConfig> implements IOUtils.IOFunction<String, T>, KindConfig {

    protected KindConfig mainKind;

    public NestedFolderDirConfigFactory(KindConfig mainKind) {
        this.mainKind = Objects.requireNonNull(mainKind);
    }

    protected ConcurrentHashMap<String, T> nestedMap = new ConcurrentHashMap<>();

    protected abstract T makeDirConfig(KindConfig kind, String folderName);
    
    @Override
    public T apply(final String folderName) throws IOException {
        return nestedMap.computeIfAbsent(folderName, name -> {
            return makeDirConfig(mainKind, folderName);
        });

    }

    @Override
    public String getConfigID() {
        return mainKind.getConfigID();
    }

    @Override
    public String getFileKind() {
        return mainKind.getFileKind();
    }

    @Override
    public String getFileOrigin() {
        return mainKind.getFileOrigin();
    }

    @Override
    public CheckedExecutor getLuceneExecutor() {
        return mainKind.getLuceneExecutor();
    }

}
