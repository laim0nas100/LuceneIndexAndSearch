package lt.lb.lucenejpa;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.persistence.EntityManager;
import lt.lb.commons.jpa.EntityFacade;
import lt.lb.lucenejpa.Forwarding.DirConfigFromKind;
import org.apache.lucene.util.IOUtils;

/**
 *
 * @author laim0nas100
 */
public class NestedFolderDirConfigFactory implements IOUtils.IOFunction<String, DirConfig>, KindConfig {

    protected KindConfig mainKind;

    public NestedFolderDirConfigFactory(KindConfig mainKind) {
        this.mainKind = Objects.requireNonNull(mainKind);
    }

    protected ConcurrentHashMap<String, DirConfig> nestedMap = new ConcurrentHashMap<>();

    @Override
    public DirConfig apply(final String folderName) throws IOException {
        return nestedMap.computeIfAbsent(folderName, name -> {
            return new DirConfigFromKind(mainKind,name);
        });

    }

    @Override
    public ScheduledExecutorService getSchedService() {
        return mainKind.getSchedService();
    }

    @Override
    public ExecutorService getService() {
        return mainKind.getService();
    }

    @Override
    public long getSecondsTimeout() {
        return mainKind.getSecondsTimeout();
    }

    @Override
    public boolean useAsync() {
        return mainKind.useAsync();
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
    public EntityManager getEntityManager() {
        return mainKind.getEntityManager();
    }

    @Override
    public EntityFacade getEntityFacade() {
        return mainKind.getEntityFacade();
    }

}
