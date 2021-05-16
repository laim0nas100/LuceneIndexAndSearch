package lt.lb.lucenejpa;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import lt.lb.commons.Java;
import lt.lb.commons.jpa.EntityFacade;
import lt.lb.commons.threads.executors.FastExecutor;
import lt.lb.commons.threads.executors.scheduled.DelayedTaskExecutor;

/**
 *
 * @author laim0nas100
 */
public class LazyDirConfig implements DirConfig {

    protected String folderName;
    protected String fileKind;
    protected String fileOrigin;
    protected Supplier<EntityManager> entityManagerSupplier;
    protected Supplier<EntityFacade> entityFacadeSupplier;
    protected ExecutorService executor = new FastExecutor(Java.getAvailableProcessors());
    protected ScheduledExecutorService scheduler = new DelayedTaskExecutor(executor);

    @Override
    public String getFolderName() {
        return folderName;
    }

    @Override
    public String getFileKind() {
        return fileKind;
    }

    @Override
    public String getFileOrigin() {
        return fileOrigin;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManagerSupplier.get();
    }

    @Override
    public EntityFacade getEntityFacade() {
        return entityFacadeSupplier.get();
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setFileKind(String fileKind) {
        this.fileKind = fileKind;
    }

    public void setFileOrigin(String fileOrigin) {
        this.fileOrigin = fileOrigin;
    }

    public void setEntityManagerSupplier(Supplier<EntityManager> entityManagerSupplier) {
        this.entityManagerSupplier = entityManagerSupplier;
    }

    public void setEntityFacadeSupplier(Supplier<EntityFacade> entityFacadeSupplier) {
        this.entityFacadeSupplier = entityFacadeSupplier;
    }

    @Override
    public ScheduledExecutorService getSchedService() {
        return scheduler;
    }

    @Override
    public ExecutorService getService() {
        return executor;
    }

}
