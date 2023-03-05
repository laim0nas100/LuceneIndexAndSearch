package lt.lb.luceneindexandsearch.splitting;

import java.io.File;

/**
 *
 * @author laim0nas100
 */
public interface DirConfig {
    
    public KindConfig getKindConfig();

    public default String getConfigID() {
        return getKindConfig().getFileOrigin() + "_" + getKindConfig().getFileKind() + "_" + getFolderName();
    }

    public default String getFullName(String name) {
        return getFolderName() + File.separator + name;
    }

    public default String tempFileName(String name) {
        return getKindConfig().getFileKind() + "_" + getFolderName() + "_" + name + "_" + System.currentTimeMillis();
    }

    /**
     * Folder name. Should be able to separate folders of the same kind in same
     * or different origins.
     *
     * @return
     */
    public String getFolderName();

}
