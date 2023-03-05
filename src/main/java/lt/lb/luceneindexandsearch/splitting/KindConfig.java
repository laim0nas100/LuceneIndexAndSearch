package lt.lb.luceneindexandsearch.splitting;

import lt.lb.uncheckedutils.concurrent.CheckedExecutor;

/**
 *
 * @author laim0nas100
 */
public interface KindConfig {
    
    CheckedExecutor getLuceneExecutor();

    public default String getConfigID(){
        return getFileOrigin() + "_" + getFileKind();
    }

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
    
    public static class SimpleKindConfig implements KindConfig{
        
        protected CheckedExecutor executor;
        protected String fileKind;
        protected String fileOrigin;

        public SimpleKindConfig(CheckedExecutor executor, String fileKind, String fileOrigin) {
            this.executor = executor;
            this.fileKind = fileKind;
            this.fileOrigin = fileOrigin;
        }
        
        

        @Override
        public CheckedExecutor getLuceneExecutor() {
            return executor;
        }

        @Override
        public String getFileKind() {
            return fileKind;
        }

        @Override
        public String getFileOrigin() {
            return fileOrigin;
        }
        
    }

}
