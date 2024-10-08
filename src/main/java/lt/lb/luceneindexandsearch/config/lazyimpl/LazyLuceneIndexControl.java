package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lt.lb.commons.Lazy;
import lt.lb.commons.containers.collections.CollectionOp;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.containers.values.LongValue;
import lt.lb.commons.containers.values.ThreadLocalValue;
import lt.lb.commons.iteration.For;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.LuceneIndexControl;
import lt.lb.luceneindexandsearch.config.LuceneSearchService;
import lt.lb.luceneindexandsearch.config.LuceneServicesResolver;
import lt.lb.luceneindexandsearch.indexing.IdMap;
import lt.lb.uncheckedutils.Checked;
import lt.lb.uncheckedutils.concurrent.CheckedExecutor;
import lt.lb.uncheckedutils.PassableException;
import lt.lb.uncheckedutils.SafeOpt;
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

    protected int batchWriteCount = 25;
    protected int batchDeleteCount = 100;
    protected Supplier<Map<Property, LuceneCachedMap<ID, D>>> cachingStrategy;
    protected boolean callGC = true;
    protected boolean updateOnAdd = true;
    protected int repetitions = 1;
    protected boolean skipVersionCheck = true;

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

    public Map<ID, D> getCurrentIDs(Property prop) throws IOException {
        Map<ID, D> ids = getLazyCache(prop).getMap();
        logger.trace("getCurrentIDs {} {}", prop, ids);
        return ids;
    }

    protected abstract CheckedExecutor getLuceneExecutor();

    protected abstract CheckedExecutor getAccessExecutor();

    protected LuceneCachedMap<ID, D> populateMap(Property prop) throws IOException {
        logger.trace("populateMap {}", prop);
        return getLuceneExecutor().call(() -> {
            return getLuceneServicesResolver().getSearch(prop)
                    .pagingSearch(new MatchAllDocsQuery(), fieldsToLoad(prop))
                    .map(doc -> documentInfoRetrieve(doc))
                    .collect(IdAndChanged.collector());
        }).map(m -> new LuceneCachedMapImpl<>(m))
                .throwIfErrorUnwrapping(IOException.class)
                .orElseGet(() -> LuceneCachedMapImpl.empty());

    }

    protected Map<Property, LuceneCachedMap<ID, D>> getNestedCachedMap() {
        return cachingStrategy.get();
    }

    protected LuceneCachedMap<ID, D> getLazyCache(Property prop) {
        logger.trace("getLazyCache {}", prop);
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

    @Override
    public Long indexedCount(Property prop) throws IOException {
        return getLuceneExecutor().call(() -> {
            return getLuceneServicesResolver().getSearch(prop).count(new MatchAllDocsQuery());
        })
                .throwIfErrorUnwrapping(IOException.class)
                .get();
    }

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
    public void updateIndexDeletion(Property folderName) throws IOException {
        List<ID> idsToDelete = getAccessExecutor().call(() -> {
            return new ArrayList<>(idsToDelete(folderName, getCurrentIDs(folderName)));

        }).throwIfErrorUnwrapping(IOException.class).orElseGet(ArrayList::new);

        logger.trace("updateIndexDeletion({},{})", folderName, idsToDelete);
        if (idsToDelete.isEmpty()) {
            return;
        }
        getLuceneExecutor().execute(() -> {
            List<List<ID>> partition = ListUtils.partition(idsToDelete, batchDeleteCount);

            LuceneServicesResolver<Property> resolver = getLuceneServicesResolver();

            try (IndexWriter indexWriter = resolver.getWriter(folderName).getIndexWriter()) {
                for (List<ID> batch : partition) {
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
    public abstract Collection<ID> idsToDelete(Property folderName, Map<ID, D> currentIDs) throws IOException;

    @Override
    public void updateIndexAddition(Property folderName) throws IOException {
        getLuceneExecutor().execute(() -> {
            for (int loop = 0; loop < Math.max(repetitions, 1); loop++) {

                Map<ID, D> idsToAdd = getAccessExecutor().call(() -> {
                    return idsToAdd(folderName, getCurrentIDs(folderName));
                }).throwIfErrorUnwrapping(IOException.class).orElseGet(HashMap::new);
                if (idsToAdd.isEmpty()) {// no change, exit
                    return;
                }

                writeIdsToIndex(updateOnAdd, folderName, idsToAdd);// TODO, multiple IDS can be present??? what
                getLazyCache(folderName).updateWith(idsToAdd);
            }
        }).throwIfErrorUnwrapping(IOException.class);
    }

    public abstract Collection<IdMap<ID>> requestIndexableMaps(Property folderName, Map<ID, D> ids) throws IOException;

    public abstract Map<ID, D> idsToAdd(Property folderName, Map<ID, D> currentIDs) throws IOException;

    protected String mainIdToString(ID id) {
        return String.valueOf(id);
    }

    protected abstract SafeOpt<ID> mainIdFromString(String string);

    protected abstract IdAndChanged<ID, D> documentInfoRetrieve(Document doc);

    protected abstract String getIndexableItemVersion(Property folderName);

    protected abstract String getIndexableItemVersionFieldName(Property folderName);

    @Override
    public void updateIndexVersion(Property folderName) throws IOException {

        if (skipVersionCheck) {
            return;
        }
        getLuceneExecutor().execute(() -> {
            for (int loop = 0; loop < Math.max(repetitions, 1); loop++) {
                LuceneServicesResolver<Property> resolver = getLuceneServicesResolver();
                LuceneSearchService search = resolver.getSearch(folderName);

                String indexableItemVersion = Objects.requireNonNull(getIndexableItemVersion(folderName));
                String indexableItemVersionFieldName = Objects.requireNonNull(getIndexableItemVersionFieldName(folderName));
                TermQuery versionQuery = new TermQuery(new Term(indexableItemVersionFieldName, indexableItemVersion));
                BooleanQuery notContainsQuery = new BooleanQuery.Builder().add(versionQuery, BooleanClause.Occur.MUST_NOT).build();

                Map<ID, D> idsToChange = search.pagingSearch(notContainsQuery, fieldsToLoad(folderName))
                        .map(doc -> documentInfoRetrieve(doc))
                        .collect(IdAndChanged.collector());
                if (idsToChange.isEmpty()) {
                    return;
                }
                writeIdsToIndex(true, folderName, idsToChange);
                getLazyCache(folderName).updateWith(idsToChange);
            }

        }).throwIfErrorUnwrapping(IOException.class);

    }

    @Override
    public void updateIndexChange(Property folderName) throws IOException {
        getLuceneExecutor().execute(() -> {
            for (int loop = 0; loop < Math.max(repetitions, 1); loop++) {
                Map<ID, D> idsToChange = getAccessExecutor().call(() -> {
                    return idsToChange(folderName, getCurrentIDs(folderName));
                }).throwIfErrorUnwrapping(IOException.class).orElse(ImmutableCollections.mapOf());
                if (idsToChange.isEmpty()) {
                    return;
                }

                writeIdsToIndex(true, folderName, idsToChange);
                getLazyCache(folderName).updateWith(idsToChange);
            }
        }).throwIfErrorUnwrapping(IOException.class);
    }

    public void writeIdsToIndex(boolean update, Property folderName, Map<ID, D> ids) throws IOException {
        getLuceneExecutor().execute(() -> {
            logger.trace("writeIdsToIndex({},{},{})", update, folderName, ids);

            LuceneServicesResolver<Property> resolver = getLuceneServicesResolver();
            DocumentFieldsConfig fieldsConfig = resolver.getReader(folderName).getDocumentFieldsConfig();
            LuceneSearchService search = resolver.getSearch(folderName);

            CollectionOp.doBatchMap(batchWriteCount, ids, batch -> {
                getAccessExecutor().call(() -> { // nested access call
                    return requestIndexableMaps(folderName, batch);
                }).map(maps -> { // out of access call
                    if (maps.isEmpty()) {
                        return 0L;
                    }
                    try (IndexWriter indexWriter = resolver.getWriter(folderName).getIndexWriter()) {
                        if (update) {
                            for (IdMap<ID> idMap : maps) {
                                Document doc = fieldsConfig.createDocument(idMap.map);
                                Term mainIdTerm = search.mainIdTerm(mainIdToString(idMap.id));
                                indexWriter.updateDocument(mainIdTerm, doc);
                            }
                        } else {
                            List<Document> docs = maps.stream().map(m -> fieldsConfig.createDocument(m.map)).collect(Collectors.toList());
                            indexWriter.addDocuments(docs);

                        }
                        return indexWriter.commit();
                    }
                }).throwIfErrorAsNested().orNull();
            });
        }).throwIfErrorUnwrapping(IOException.class);

    }

    public abstract Map<ID, D> idsToChange(Property folderName, Map<ID, D> currentIDs) throws IOException;

    public abstract Set<String> fieldsToLoad(Property folderName);

    @Override
    public void periodicMaintenance() throws IOException {

        initOrExpandNested();
        List<Property> nestedKeys = getNestedKeys();// can be just populated
        List<Throwable> errors = new ArrayList<>();
        for (Property key : nestedKeys) {

            getLuceneExecutor().execute(() -> {
                periodicMaintenance(key);
            }).peekError(err -> {
                logger.error("Error at " + key, err);
                errors.add(err);
            });

        }

        if (callGC) {
            System.gc();
        }
        if (!errors.isEmpty()) {
            throw new PassableException(errors.size() + " errors has occured during maintenance. Check logs.");
        }

    }

    @Override
    public void periodicMaintenance(Property folder) throws IOException {
        getLuceneExecutor().execute(() -> {
            getNestedCachedMap().remove(folder);
            LuceneIndexControl.super.periodicMaintenance(folder);
            getNestedCachedMap().remove(folder);
        }).throwIfErrorUnwrapping(IOException.class);

    }

    @Override
    public void updateIndexesDeletions() throws IOException {
        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexesDeletions();
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void updateIndexesAddition() throws IOException {

        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexesAddition();
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void updateIndexesChange() throws IOException {
        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexesChange();
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void updateIndexPrepare(Property folder) throws IOException {
        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexPrepare(folder);
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void updateIndexesPrepare() throws IOException {
        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexesPrepare();
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void updateIndexCleanup(Property folder) throws IOException {
        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexCleanup(folder);
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void updateIndexesCleanup() throws IOException {
        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexesCleanup();
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void updateIndexVersions() throws IOException {
        getLuceneExecutor().execute(() -> {
            LuceneIndexControl.super.updateIndexVersions();
        }).throwIfErrorUnwrapping(IOException.class);
    }

    @Override
    public void deduplify(Property folderName) throws IOException {
        if (isPresentAndWritable(folderName).isEmpty()) {
            return;
        }
        getLuceneExecutor().execute(() -> {
            LuceneSearchService search = getLuceneServicesResolver().getSearch(folderName);
            Map<ID, LongValue> countMap = new HashMap<>();
            search.search(new MatchAllDocsQuery()).map(m -> m.get(search.mainIdField()))
                    .map(m -> SafeOpt.of(m).flatMap(this::mainIdFromString)).filter(f -> f.isPresent())
                    .map(m -> m.get()).forEach(id -> {
                countMap.computeIfAbsent(id, i -> new LongValue(0)).incrementAndGet();
            });
            List<ID> toRemove = new ArrayList<>();
            For.entries().iterate(countMap, (id, count) -> {
                if (count.get() >= 2) {
                    toRemove.add(id);
                }
            });

            if (!toRemove.isEmpty()) {
                getLuceneExecutor().execute(() -> {
                    List<List<ID>> partition = ListUtils.partition(toRemove, batchWriteCount);

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

        }).throwIfErrorAsNested();

    }

}
