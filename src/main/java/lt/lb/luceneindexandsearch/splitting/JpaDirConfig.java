package lt.lb.luceneindexandsearch.splitting;

import javax.persistence.EntityManager;
import lt.lb.commons.jpa.EntityFacade;
import lt.lb.uncheckedutils.concurrent.CheckedExecutor;

/**
 *
 * @author laim0nas100
 */
public interface JpaDirConfig extends DirConfig {

    public default boolean bufferedJPAStreams() {
        return false;
    }

    public default EntityManager getEntityManager() {
        return getEntityFacade().getEntityManager();
    }

    public EntityFacade getEntityFacade();

    public default CheckedExecutor getLuceneExecutor() {
        return getKindConfig().getLuceneExecutor();
    }
    
    public default String getFileKind(){
        return getKindConfig().getFileKind();
    }
    
    public default String getFileOrigin(){
        return getKindConfig().getFileOrigin();
    }
}
