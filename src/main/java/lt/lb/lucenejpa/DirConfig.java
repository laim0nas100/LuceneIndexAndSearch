package lt.lb.lucenejpa;

/**
 *
 * @author laim0nas100
 */
public interface DirConfig extends KindConfig {

    @Override
    public default String getConfigID() {
        return getFileOrigin() + "_" + getFileKind() + "_" + getFolderName();
    }

    public default boolean bufferedJPAStreams() {
        return false;
    }

    public default String getFullName(String name) {
        return getFolderName() + "/" + name;
    }

    public default String tempFileName(String name) {
        return getFileKind() + "_" + getFolderName() + "_" + name + "_" + System.currentTimeMillis();
    }

    /**
     * Folder name. Should be able to separate folders of the same kind in same
     * or different origins.
     *
     * @return
     */
    public String getFolderName();

}
