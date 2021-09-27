package lt.lb.luceneindexandsearch.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lt.lb.commons.iteration.PagedIteration;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.luceneindexandsearch.config.indexing.IndexingReaderConfig;
import lt.lb.uncheckedutils.Checked;
import lt.lb.uncheckedutils.func.UncheckedFunction;
import lt.lb.uncheckedutils.func.UncheckedRunnable;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author laim0nas100
 */
public interface LuceneSearchService {

    public default int pageSize() {
        return 10000;
    }

    public IndexingReaderConfig getIndexingConfig();

    public default UncheckedFunction<String, Query> getContentQueryMaker() {
        DocumentFieldsConfig docFields = getIndexingConfig().getDocumentFieldsConfig();
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(
                docFields.getContentFieldsArray(),
                getIndexingConfig().getSearchAnalyzer()
        );

        return str -> multiFieldQueryParser.parse(str);
    }

    public default Query makeContentQuery(String searchStr) {
        return getContentQueryMaker().apply(searchStr);
    }

    public default Query makeMainIdQuery(String searchStr) {
        return new TermQuery(maidIdTerm(searchStr));
    }
    
    public default String mainIdField(){
        DocumentFieldsConfig docFields = getIndexingConfig().getDocumentFieldsConfig();
        String mainIdFieldName = docFields.getMainIdFieldName();
        return mainIdFieldName;
    }

    public default Term maidIdTerm(String searchString) {
        return new Term(mainIdField(), searchString);
    }

    public default boolean containsMainId(String id) throws IOException {
        return count(makeMainIdQuery(id)) > 0;
    }

    public default long count(Query query) throws IOException {
        try (IndexReader indexReader = getIndexingConfig().getIndexReader()) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            return searcher.count(query);
        }
    }

    public default Stream<Document> search(Query query) throws IOException {
        return pagingSearch(query, null);
    }

    public default Stream<Document> pagingSearch(Query query, Set<String> fieldsToLoad) throws IOException {

        IndexReader indexReader = getIndexingConfig().getIndexReader();
        IndexSearcher searcher = new IndexSearcher(indexReader);
        final int pageSize = pageSize();

        Iterable<Document> iter = new PagedIteration<TopDocs, Document>() {
            @Override
            public TopDocs getFirstPage() {
                return Checked.uncheckedCall(() -> {
                    return searcher.search(query, pageSize);
                });
            }

            @Override
            public Iterator<Document> getItems(TopDocs info) {

                return Checked.uncheckedCall(() -> {

                    List<Document> result = new ArrayList<>(info.scoreDocs.length);
                    for (ScoreDoc scoreDoc : info.scoreDocs) {
                        if (fieldsToLoad == null || fieldsToLoad.isEmpty()) {
                            result.add(searcher.doc(scoreDoc.doc));
                        } else {
                            result.add(searcher.doc(scoreDoc.doc, fieldsToLoad));
                        }
                    }
                    return result.iterator();
                });
            }

            @Override
            public TopDocs getNextPage(TopDocs info) {
                return Checked.uncheckedCall(() -> {
                    int len = info.scoreDocs.length;
                    // assume length > 0;
                    ScoreDoc lastDoc = info.scoreDocs[len - 1];
                    return searcher.searchAfter(lastDoc, query, pageSize);
                });
            }

            @Override
            public boolean hasNextPage(TopDocs info) {
                boolean hasNext = info.scoreDocs.length == pageSize;
                if (!hasNext) {
                    Checked.uncheckedRun(() -> {
                        indexReader.close();
                    });
                }
                return hasNext;
            }
        };

        return ReadOnlyIterator.of(iter)
                .withEnsuredCloseOperation((UncheckedRunnable) () -> {
                    indexReader.close();
                }).toStream();

    }

}
