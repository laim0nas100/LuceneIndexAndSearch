package lt.lb.luceneindexandsearch.config;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lt.lb.commons.Java;
import lt.lb.commons.containers.tuples.Tuple;
import lt.lb.commons.threads.executors.FastExecutor;
import lt.lb.commons.threads.executors.TaskBatcher;
import lt.lb.commons.threads.executors.TaskBatcher.BatchRunSummary;
import lt.lb.luceneindexandsearch.config.indexing.IndexingMultiReaderConfig;
import lt.lb.luceneindexandsearch.config.indexing.IndexingReaderConfig;
import lt.lb.luceneindexandsearch.config.indexing.IndexingWriterConfig;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

/**
 *
 * @author laim0nas100
 * @param <Property>
 */
public interface LuceneServicesResolver<Property> {

    public GrowingMultiIndexingConfig<Property> getMultiIndexingConfig();

    public IndexingMultiReaderConfig getMultiReaderConfig();
    
    public LuceneTaskExecutor getLuceneTaskExecutor();

    public default IndexingReaderConfig getReader(Property prop) throws IOException {
        return getMultiIndexingConfig().resolve(prop);
    }

    public default IndexingWriterConfig getWriter(Property prop) throws IOException {
        return getMultiIndexingConfig().resolve(prop);
    }

    public default LuceneSearchService getSearch(Property prop) throws IOException {
        return makeSearchService(getMultiIndexingConfig().resolve(prop));
    }

    public default LuceneSearchService makeSearchService(IndexingReaderConfig readingConfig) {
        Objects.requireNonNull(readingConfig);
        return () -> readingConfig;
    }

    public default Tuple<Collection<Document>, BatchRunSummary> fastThreadedSearch(Query query) throws IOException {
        return fastThreadedSearch(new FastExecutor(Java.getAvailableProcessors()), query, null);
    }

    public default Tuple<Collection<Document>, BatchRunSummary> fastThreadedSearch(Executor exe, Query query, Set<String> fields) throws IOException {
        Set<Property> keySet = getMultiIndexingConfig().getIndexingConfigMap().keySet();
        TaskBatcher batcher = new TaskBatcher(exe);
        ConcurrentLinkedDeque<Document> docs = new ConcurrentLinkedDeque<>();
        for (Property prop : keySet) {

            LuceneSearchService search = getSearch(prop);
            batcher.execute(() -> {
                List<Document> collect = search.pagingSearch(query, fields).collect(Collectors.toList());
                docs.addAll(collect);
                return null;
            });

        }

        TaskBatcher.BatchRunSummary summary = batcher.awaitTolerateFails();

        return new Tuple<>(docs, summary);
    }
    
    public default Tuple<Long, BatchRunSummary> fastThreadedCount(Executor exe, Query query) throws IOException {
        Set<Property> keySet = getMultiIndexingConfig().getIndexingConfigMap().keySet();
        TaskBatcher batcher = new TaskBatcher(exe);
        AtomicLong atomicCount = new AtomicLong(0L);
        for (Property prop : keySet) {

            LuceneSearchService search = getSearch(prop);
            batcher.execute(() -> {
                atomicCount.addAndGet(search.count(query));
                return null;
            });

        }

        TaskBatcher.BatchRunSummary summary = batcher.awaitTolerateFails();

        return new Tuple<>(atomicCount.get(), summary);
    }

    public default LuceneSearchService getMultiSearch() throws IOException {
        IndexingMultiReaderConfig multi = getMultiReaderConfig();
        IndexingReaderConfig withNewReader = multi.withNewReader(() -> {
            return IndexingMultiReaderConfig.makeMultiReader(multi.getReaderConfigs());
        });
        return makeSearchService(withNewReader);
    }

}
