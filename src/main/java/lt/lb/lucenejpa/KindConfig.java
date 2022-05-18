package lt.lb.lucenejpa;

import javax.persistence.EntityManager;
import lt.lb.commons.jpa.EntityFacade;
import lt.lb.uncheckedutils.concurrent.CheckedExecutor;

/**
 *
 * @author laim0nas100
 */
public interface KindConfig {
    
    CheckedExecutor getLuceneExecutor();

    public default long getSecondsTimeout() {
        return 10;
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
