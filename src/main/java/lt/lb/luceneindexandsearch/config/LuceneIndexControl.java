package lt.lb.luceneindexandsearch.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lt.lb.lucenejpa.SyncDirectory;
import lt.lb.uncheckedutils.SafeOpt;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.CheckIndex.Status;
import org.apache.lucene.index.IndexWriter;

/**
 *
 *
 * @param <Property> a way to differentiate folders
 * @param <ID> type of main id
 * @param <D> type of change time stamp to see which is newer
 * @author laim0nas100
 */
public interface LuceneIndexControl<Property, ID, D extends Comparable<D>> {

    /**
     * Lazy init and resolve a {@link IndexingConfig} and get associated
     * directory
     *
     * @param prop
     * @return
     * @throws IOException
     */
    public default SyncDirectory resolveDirectory(Property prop) throws IOException {
        return resolveConfig(prop).getDirectory();
    }

    /**
     * Lazy init and resolve a {@link IndexingConfig}
     *
     * @param prop
     * @return
     * @throws IOException
     */
    public default IndexingConfig resolveConfig(Property prop) throws IOException {
        return getLuceneServicesResolver().getMultiIndexingConfig().resolve(prop);
    }

    public default Status checkIndex(Property prop, StringBuilder sb, boolean verbose) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Status checked;
        try (PrintStream printStream = new PrintStream(baos, true); CheckIndex check = new CheckIndex(resolveDirectory(prop))) {
            check.setInfoStream(printStream, verbose);

            checked = check.checkIndex();

        }
        sb.append(baos.toString(StandardCharsets.UTF_8.name()));
        return checked;
    }

    public default Status checkChecksums(Property prop, StringBuilder sb, boolean verbose) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Status checked;
        try (PrintStream printStream = new PrintStream(baos, true); CheckIndex check = new CheckIndex(resolveDirectory(prop))) {
            check.setChecksumsOnly(true);
            check.setInfoStream(printStream, verbose);

            checked = check.checkIndex();

        }
        sb.append(baos.toString(StandardCharsets.UTF_8.name()));
        return checked;
    }

    public default void fixIndex(Property prop, StringBuilder sb, boolean verbose) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(baos, true); CheckIndex check = new CheckIndex(resolveDirectory(prop))) {

            check.setInfoStream(printStream, verbose);
            check.exorciseIndex(check.checkIndex());

        }
        sb.append(baos.toString(StandardCharsets.UTF_8.name()));
    }

    public SafeOpt<Property> findCorrectFolder(ID id);

    public static interface IdAndChanged<ID, D extends Comparable<D>> {

        public ID getID();

        public D getChanged();

        public String getVersion();

        public static <ID, D extends Comparable<D>> IdAndChanged<ID, D> of(ID id, D change, String version) {
            Objects.requireNonNull(id, "ID must not be null");
            Objects.requireNonNull(change, "Changed must not be null");
            Objects.requireNonNull(version, "Version must not be null");
            return new IdAndChanged<ID, D>() {
                @Override
                public ID getID() {
                    return id;
                }

                @Override
                public D getChanged() {
                    return change;
                }

                @Override
                public String getVersion() {
                    return version;
                }
            };
        }
    }

    public LuceneServicesResolver<Property> getLuceneServicesResolver();

    public Map<ID, D> getCachedIDs(Property prop) throws IOException;

    public Optional<D> getLastChange(Property prop) throws IOException;

    public default List<Property> getNestedKeys() {
        return new ArrayList<>((getLuceneServicesResolver().getMultiIndexingConfig().getIndexingConfigMap().keySet()));
    }

    public Long indexedCount(Property prop) throws IOException;

    public default Long indexedCount() throws IOException {
        Long sum = 0L;
        for (Property prop : getNestedKeys()) {
            sum += indexedCount(prop);
        }
        return sum;
    }

    public Long indexableCount(Property prop) throws IOException;

    public default Long indexableCount() throws IOException {
        Long sum = 0L;
        for (Property prop : getNestedKeys()) {
            sum += indexableCount(prop);
        }
        return sum;
    }

    public void idSanityCheck(Property prop, StringBuilder sb) throws IOException;

    /**
     * Delete every Lucene file associated with this folder
     *
     * @param folderName
     * @throws java.io.IOException
     */
    public void deleteFolder(Property folderName) throws IOException;

    public default void deleteFolders() throws IOException {
        for (Property key : getNestedKeys()) {
            deleteFolder(key);
        }
    }

    /**
     * Query information and then use cached ids to check if any ids should be
     * deleted, then delete them and update the index
     *
     * @param folderName
     * @throws java.io.IOException
     */
    public void updateIndexDeletion(Property folderName) throws IOException;

    public default void updateIndexesDeletions() throws IOException {
        for (Property key : getNestedKeys()) {
            updateIndexDeletion(key);
        }
    }

    /**
     * Query information and then use cached ids to check if any ids should be
     * added, then add them and update the index
     *
     * @param folderName
     * @throws java.io.IOException
     */
    public void updateIndexAddition(Property folderName) throws IOException;

    public default void updateIndexesAddition() throws IOException {
        for (Property key : getNestedKeys()) {
            updateIndexAddition(key);
        }
    }

    /**
     * Query information and then use cached ids to check if any ids should be
     * changed, then add the changes (by deleting and re-adding changed) and
     * update the index
     *
     * @param folderName
     * @throws java.io.IOException
     */
    public void updateIndexChange(Property folderName) throws IOException;

    public default void updateIndexesChange() throws IOException {
        for (Property key : getNestedKeys()) {
            updateIndexChange(key);
        }
    }

    public default void updateIndexVersions() throws IOException {
        for (Property prop : getNestedKeys()) {
            updateIndexVersion(prop);
        }
    }

    /**
     * Query information and then use cached ids to check if any indexed item
     * versions should be re-indexed and updated to newest.
     *
     * @param folderName
     * @throws java.io.IOException
     */
    public void updateIndexVersion(Property folderName) throws IOException;

    public default void periodicMaintenance() throws IOException {

        updateIndexesPrepare();
        updateIndexesAddition();
        updateIndexesChange();
        updateIndexesDeletions();
        updateIndexesCleanup();

    }

    public void initOrExpandNested() throws IOException;

    /**
     * Initializes directory if was empty. Mainly to write segments file.
     *
     * @param folder
     * @throws IOException
     */
    public default void updateIndexPrepare(Property folder) throws IOException {
        IndexingConfig indexing = resolveConfig(folder);
        SyncDirectory dir = indexing.getDirectory();
        if (dir.isReadOnly()) {
            return;
        }
        dir.syncLocal();
        if (dir.isEmpty()) {
            try (IndexWriter indexWriter = indexing.getIndexWriter()) {
                indexWriter.commit();
            }
        }
    }

    public default void updateIndexesPrepare() throws IOException {
        for (Property prop : getNestedKeys()) {
            updateIndexPrepare(prop);
        }
    }

    /**
     * Cleans up directory after maintenance. Mainly calls syncRemote.
     *
     * @param folder
     * @throws IOException
     */
    public default void updateIndexCleanup(Property folder) throws IOException {
        IndexingConfig indexing = resolveConfig(folder);
        SyncDirectory dir = indexing.getDirectory();
        if (dir.isReadOnly()) {
            return;
        }
        dir.syncRemote();
    }

    public default void updateIndexesCleanup() throws IOException {
        for (Property prop : getNestedKeys()) {
            updateIndexCleanup(prop);
        }
    }

    public default void periodicMaintenance(Property folder) throws IOException {
        if (resolveDirectory(folder).isReadOnly()) {
            return;
        }
        updateIndexPrepare(folder);
        updateIndexAddition(folder);
        updateIndexChange(folder);
        updateIndexDeletion(folder);
        updateIndexCleanup(folder);
    }

}
