package lt.lb.lucenejpa;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.persistence.EntityManager;
import lt.lb.commons.jpa.EntityFacade;

/**
 *
 * @author laim0nas100
 */
public class Forwarding {

    public static interface ForwardingKindConfig extends KindConfig {

        public KindConfig getDelegate();

        @Override
        public default ScheduledExecutorService getSchedService() {
            return getDelegate().getSchedService();
        }

        @Override
        public default ExecutorService getService() {
            return getDelegate().getService();
        }

        @Override
        public default String getConfigID() {
            return getDelegate().getConfigID();
        }

        @Override
        public default String getFileKind() {
            return getDelegate().getFileKind();
        }

        @Override
        public default String getFileOrigin() {
            return getDelegate().getFileOrigin();
        }

        @Override
        public default EntityFacade getEntityFacade() {
            return getDelegate().getEntityFacade();
        }

        @Override
        public default EntityManager getEntityManager() {
            return getDelegate().getEntityManager();
        }

        @Override
        public default boolean useAsync() {
            return getDelegate().useAsync();
        }

        @Override
        public default long getSecondsTimeout() {
            return getDelegate().getSecondsTimeout();
        }

    }

    public static interface ForwardingDirConfig extends ForwardingKindConfig, DirConfig {

        @Override
        public DirConfig getDelegate();

        @Override
        public default String getConfigID() {
            return getDelegate().getConfigID();
        }

        @Override
        public default String getFolderName() {
            return getDelegate().getFolderName();
        }

        @Override
        public default String tempFileName(String name) {
            return getDelegate().tempFileName(name);
        }

        @Override
        public default String getFullName(String name) {
            return getDelegate().getFullName(name);
        }

        @Override
        public default boolean bufferedJPAStreams() {
            return getDelegate().bufferedJPAStreams();
        }

    }

    public static class DirConfigFromKind implements DirConfig, ForwardingKindConfig {

        protected final String folderName;
        protected final KindConfig kind;

        public DirConfigFromKind(KindConfig kind, String folderName) {
            this.kind = Objects.requireNonNull(kind);
            this.folderName = Objects.requireNonNull(folderName);
        }

        @Override
        public String getFolderName() {
            return folderName;
        }


        @Override
        public String toString() {
            return getConfigID();
        }

        @Override
        public KindConfig getDelegate() {
            return kind;
        }

        @Override
        public String getConfigID() {
            return DirConfig.super.getConfigID();
        }
        
        

    }
}
