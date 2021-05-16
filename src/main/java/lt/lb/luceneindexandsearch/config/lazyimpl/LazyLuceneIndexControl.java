package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.Lazy;
import lt.lb.commons.containers.values.ThreadLocalValue;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.IndexingConfig;
import lt.lb.luceneindexandsearch.config.LuceneIndexControl;
import lt.lb.luceneindexandsearch.config.LuceneSearchService;
import lt.lb.luceneindexandsearch.config.LuceneServicesResolver;
import lt.lb.lucenejpa.SyncDirectory;
import lt.lb.uncheckedutils.Checked;
import lt.lb.uncheckedutils.CheckedExecutor;
import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author laim0nas100
 * @param <Property> a way to split directory
 * @param <ID> main id type
 * @param <D> change timestamp comparable type
 */
public abstract class LazyLuceneIndexControl<Property, ID, D extends Comparable<D>> implements LuceneIndexControl<Property, ID, D> {

    public static <Property, ID, D extends Comparable<D>> Supplier<Map<Property, LuceneCachedMap<ID, D>>> concurrentMap() {
        return new Lazy<>(ConcurrentHashMap::new);
    }

    public static <Property, ID, D extends Comparable<D>> Supplier<Map<Property, LuceneCachedMap<ID, D>>> threadLocalMap() {
        return new ThreadLocalValue<>(HashMap::new);
    }

    public static final Logger logger = LogManager.getLogger(LazyLuceneIndexControl.class);
    protected LuceneServicesResolver<Property> luceneServices;

    protected int batchWriteCount = 100;
    protected Supplier<Map<Property, LuceneCachedMap<ID, D>>> cachingStrategy;
    protected boolean clearNestedCacheMapEveryCycle = true;
    protected boolean callGC = true;

    public LazyLuceneIndexControl(Supplier<Map<Property, LuceneCachedMap<ID, D>>> cachingStrategy) {
        this.cachingStrategy = Objects.requireNonNull(cachingStrategy);
    }

    @Override
    public LuceneServicesResolver<Property> getLuceneServicesResolver() {
        return luceneServices;
    }

    @Override
    public Optional<D> getLastChange(Property prop) {
        return Optional.ofNullable(getLazyCache(prop)).flatMap(m -> m.getLastChanged());
    }

    public Set<ID> getCurrentIDs(Property prop) throws IOException {
        HashSet<ID> ids = new HashSet<>(getLazyCache(prop).getMap().keySet());
        logger.trace("getCurrentIDs {} {}", prop, ids);
        return ids;
    }

    protected abstract CheckedExecutor getLuceneExecutor();

    protected abstract CheckedExecutor getAccessExecutor();

    protected LuceneCachedMap<ID, D> populateMap(Property prop) throws IOException {
        Map<ID, D> map = new HashMap<>();
        getLuceneExecutor().execute(() -> {
            getLuceneServicesResolver().getSearch(prop)
                    .pagingSearch(new MatchAllDocsQuery(), fieldsToLoad(prop))
                    .map(doc -> documentInfoRetrieve(doc))
                    .forEach(changed -> {
                        map.putIfAbsent(changed.getID(), changed.getChanged());
                    });
        }).throwIfErrorUnwrapping(IOException.class);

        return new LuceneCachedMapImpl<>(map);
    }

    protected Map<Property, LuceneCachedMap<ID, D>> getNestedCachedMap() {
        return cachingStrategy.get();
    }

    protected LuceneCachedMap<ID, D> getLazyCache(Property prop) {
        return getNestedCachedMap().computeIfAbsent(prop, k -> Checked.uncheckedCall(() -> populateMap(k)));
    }

    @Override
    public Map<ID, D> getCachedIDs(Property prop) {
        return getLazyCache(prop).getMap();
    }

    @Override
    public void deleteFolder(Property folderName) throws IOException {
        getLuceneExecutor().execute(() -> {
            deleteFolderLogic(folderName);
            getNestedCachedMap().remove(folderName);
        }).throwIfErrorUnwrapping(IOException.class);

    }

    public abstract void deleteFolderLogic(Property folderName) throws IOException;

    protected Query idsToQuery(Collection<ID> ids, Property folderName) throws IOException {
        if (ids.isEmpty()) {
            return new MatchNoDocsQuery();
        }

        LuceneServicesResolver<Property> resolver = getLuceneServicesResolver();
        LuceneSearchService search = resolver.getSearch(folderName);

        List<BytesRef> collect = ids.stream()
                .map(id -> mainIdToString(id))
                .distinct()
                .map(id -> new BytesRef(id))
                .collect(Collectors.toList());
        return new TermInSetQuery(search.mainIdField(), collect);
//        BooleanQuery.Builder builder = new BooleanQuery.Builder();
//        ids.stream()
//                .limit(1024)
//                .map(id -> mainIdToString(id))
//                .distinct()
//                .map(idString -> search.makeMainIdQuery(idString))
//                .forEach(q -> {
//                    builder.add(q, BooleanClause.Occur.SHOULD);
//                });
//
//        return builder.setMinimumNumberShouldMatch(1).build();

    }

    @Override
    public void updateIndexDeletions(Property folderName) throws IOException {
        List<ID> idsToDelete = getAccessExecutor().call(() -> {
            return new ArrayList<>(idsToDelete(folderName, getCurrentIDs(folderName)));

        }).throwIfErrorUnwrapping(IOException.class).get();
        if (idsToDelete.isEmpty()) {
            return;
        }
        getLuceneExecutor().execute(() -> {
            List<List<ID>> partition = ListUtils.partition(idsToDelete, batchWriteCount);

            LuceneServicesResolver<Property> resolver = getLuceneServicesResolver();
            for (List<ID> batch : partition) {

                try (IndexWriter indexWriter = resolver.getWriter(folderName).getIndexWriter()) {
                    Query query = idsToQuery(batch, folderName);
                    indexWriter.deleteDocuments(query);
                    indexWriter.commit();
                }
            }
        }).throwIfErrorUnwrapping(IOException.class);

    }

    /**
     * Return what ID should be deleted from the index
     *
     * @param currentIDs
     * @param folderName
     * @return
     * @throws IOException
     */
    public abstract Collection<ID> idsToDelete(Property folderName, Set<ID> currentIDs) throws IOException;

    @Override
    public void updateIndexAddition(Property folderName) throws IOException {
        Map<ID, D> idsToAdd = getAccessExecutor().call(() -> {
            return idsToAdd(folderName, getCurrentIDs(folderName));
        }).throwIfErrorUnwrapping(IOException.class).get();
        if (idsToAdd.isEmpty()) {
            return;
        }

        writeIdsToIndex(false, folderName, new ArrayList<>(idsToAdd.keySet()));
    }

    public static class IdMap<ID> {

        public final Map<String, String> map;
        public final ID id;

        public IdMap(ID id, Map<String, String> map) {
            this.map = Objects.requireNonNull(map);
            this.id = Objects.requireNonNull(id);
        }

    }

    public abstract Collection<IdMap<ID>> requestIndexableMaps(Property folderName, Set<ID> ids) throws IOException;

    public abstract Map<ID, D> idsToAdd(Property folderName, Set<ID> currentIDs) throws IOException;

    protected String mainIdToString(ID id) {
        return String.valueOf(id);
    }

    protected abstract IdAndChanged<ID, D> documentInfoRetrieve(Document doc);

    protected abstract String getIndexableItemVersion(Property folderName);

    protected abstract String getIndexableItemVersionFieldName(Property folderName);

    @Override
    public void updateIndexVersions() throws IOException {
        for (Property prop : getNestedKeys()) {
            updateIndexVersion(prop);
        }
    }

    @Override
    public void updateIndexVersion(Property folderName) throws IOException {

        getLuceneExecutor().execute(() -> {
            LuceneServicesResolver<Property> resolver = getLuceneServicesResolver();
            LuceneSearchService search = resolver.getSearch(folderName);

            String indexableItemVersion = Objects.requireNonNull(getIndexableItemVersion(folderName));
            String indexableItemVersionFieldName = Objects.requireNonNull(getIndexableItemVersionFieldName(folderName));
            TermQuery versionQuery = new TermQuery(new Term(indexableItemVersionFieldName, indexableItemVersion));
            BooleanQuery notContainsQuery = new BooleanQuery.Builder().add(versionQuery, BooleanClause.Occur.MUST_NOT).build();
            Stream<Document> differentVersionDocuments = search.pagingSearch(notContainsQuery, fieldsToLoad(folderName));

            List<ID> idsToChange = differentVersionDocuments.map(doc -> this.documentInfoRetrieve(doc)).map(m -> m.getID()).collect(Collectors.toList());

            writeIdsToIndex(true, folderName, idsToChange);
        }).throwIfErrorUnwrapping(IOException.class);

    }

    @Override
    public void updateIndexChange(Property folderName) throws IOException {
        Map<ID, D> idsToChange = getAccessExecutor().call(() -> {
            return idsToChange(folderName, getCurrentIDs(folderName));
        }).throwIfErrorUnwrapping(IOException.class).get();
        if (idsToChange.isEmpty()) {
            return;
        }

        writeIdsToIndex(true, folderName, new ArrayList<>(idsToChange.keySet()));
    }

    public void writeIdsToIndex(boolean update, Property folderName, List<ID> ids) throws IOException {
        getLuceneExecutor().execute(() -> {
            List<List<ID>> partition = ListUtils.partition(ids, batchWriteCount);
            LuceneServicesResolver<Property> resolver = getLuceneServicesResolver();
            DocumentFieldsConfig fieldsConfig = resolver.getReader(folderName).getDocumentFieldsConfig();
            LuceneSearchService search = resolver.getSearch(folderName);

            for (List<ID> batch : partition) {

                getAccessExecutor().call(() -> { // nested access call
                    return requestIndexableMaps(folderName, new HashSet<>(batch));
                }).map(maps -> { // out of access call
                    if (maps.isEmpty()) {
                        return null;
                    }
                    try (IndexWriter indexWriter = resolver.getWriter(folderName).getIndexWriter()) {
                        for (IdMap<ID> idMap : maps) {
                            Document doc = fieldsConfig.createDocument(idMap.map);
                            if (update) {
                                Term maidIdTerm = search.maidIdTerm(mainIdToString(idMap.id));
                                indexWriter.updateDocument(maidIdTerm, doc);
                            } else {
                                indexWriter.addDocument(doc);
                            }

                        }
                        indexWriter.commit();
                    }
                    return null;
                }).throwIfErrorAsNested();

            }
        }).throwIfErrorUnwrapping(IOException.class);

    }

    public abstract Map<ID, D> idsToChange(Property folderName, Set<ID> currentIDs) throws IOException;

    public abstract Set<String> fieldsToLoad(Property folderName);

    @Override
    public void periodicMaintenance() throws IOException {

        initOrExpandNested();
        if (clearNestedCacheMapEveryCycle) {
            getNestedCachedMap().clear();
        }
        List<Property> nestedKeys = getNestedKeys();// can be just populated
        List<Throwable> errors = new ArrayList<>();
        for (Property key : nestedKeys) {

            getLuceneExecutor().execute(() -> {
                IndexingConfig indexing = resolveConfig(key);
                SyncDirectory dir = indexing.getDirectory();
                dir.syncLocal();

                if (dir.isReadOnly()) {
                    return;
                }

                if (dir.isEmpty()) {
                    try (IndexWriter indexWriter = indexing.getIndexWriter()) {
                        indexWriter.commit();
                    }
                }
                updateIndexAddition(key);
                updateIndexChange(key);
                updateIndexDeletions(key);
//                updateIndexVersion(key);

                dir.syncRemote();
                dir.close();

            }).peekError(err -> {
                logger.error("Error at " + key, err);
                errors.add(err);
            });

        }

        if (clearNestedCacheMapEveryCycle) {
            getNestedCachedMap().clear();
        }
        if (callGC) {
            System.gc();
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException(errors.size() + " errors has occured during maintenance. Check logs.");
        }

    }

}
