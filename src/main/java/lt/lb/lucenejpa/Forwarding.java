package lt.lb.lucenejpa;

import lt.lb.luceneindexandsearch.splitting.KindConfig;
import lt.lb.luceneindexandsearch.splitting.DirConfig;
import java.util.Objects;
import lt.lb.uncheckedutils.concurrent.CheckedExecutor;

/**
 *
 * @author laim0nas100
 */
public class Forwarding {

    public static interface ForwardingKindConfig extends KindConfig {

        public KindConfig getDelegate();

        @Override
        public default CheckedExecutor getLuceneExecutor() {
            return getDelegate().getLuceneExecutor();
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

        @Override
        public KindConfig getKindConfig() {
            return getDelegate();
        }

    }
}
