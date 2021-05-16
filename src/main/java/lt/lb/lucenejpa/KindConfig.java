package lt.lb.lucenejpa;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.persistence.EntityManager;
import lt.lb.commons.jpa.EntityFacade;

/**
 *
 * @author laim0nas100
 */
public interface KindConfig {

    public ScheduledExecutorService getSchedService();

    public ExecutorService getService();

    public default long getSecondsTimeout() {
        return 10;
    }

    public default boolean useAsync() {
        return false;
    }

    public String getConfigID();

    /**
     * Indexing content kind. The type of resource that is being indexed. Should
     * be able to filter all folders based on this kind
     *
     * @return
     */
    public String getFileKind();

    /**
     * Indexing content origin. Should be able to filter all kinds based on this
     * origin.
     *
     * @return
     */
    public String getFileOrigin();

    public default EntityManager getEntityManager() {
        return getEntityFacade().getEntityManager();
    }

    public EntityFacade getEntityFacade();

}
