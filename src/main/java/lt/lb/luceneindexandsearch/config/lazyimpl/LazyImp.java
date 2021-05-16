/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import lt.lb.commons.containers.caching.lazy.LazyProxy;
import lt.lb.luceneindexandsearch.LuceneUtil;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

/**
 *
 * @author laim0nas100
 */
public class LazyImp {

    public static DirectoryReader updateDirectoryReader(Directory dir, DirectoryReader old) throws IOException {

        if (LuceneUtil.isClosed(old)) {
            return DirectoryReader.open(dir);
        } else {
            if (old.isCurrent()) {
                return old;
            } else {
                DirectoryReader changed = DirectoryReader.openIfChanged(old);
                if (changed == null) {
                    return old;
                } else {
                    old.close();
                    return changed;
                }
            }
        }
    }

    public static IndexWriter createNewIndexWriter(IndexWriterConfig config, Directory dir) throws IOException {
        Objects.requireNonNull(config, "IndexWriterConfig is null");
        Objects.requireNonNull(dir, "Directory is null");
        return new IndexWriter(dir, config);
    }

    public static IndexWriter updateIndexWriter(IndexWriterConfig config, Directory dir, IndexWriter oldWriter) throws IOException {
        if (oldWriter == null) {
            return createNewIndexWriter(config, dir);
        }
        return oldWriter.isOpen() ? oldWriter : createNewIndexWriter(config, dir);
    }

    public static class LazyReaderProvider implements Supplier<IndexReader> {

        public LazyProxy<? extends Directory> lazyDir;
        public LazyProxy<DirectoryReader> lazyIndexReader;

        public LazyReaderProvider(LazyProxy<? extends Directory> lazyDirectory) {
            lazyDir = Objects.requireNonNull(lazyDirectory);
            lazyIndexReader = lazyDir
                    .map((t, dir) -> updateDirectoryReader(dir, t));

        }

        @Override
        public IndexReader get() {
            return lazyIndexReader.get();
        }

    }

    public static class LazyWriterProvider implements Supplier<IndexWriter> {

        public LazyProxy<? extends Directory> lazyDir;
        public LazyProxy<IndexWriter> lazyIndexWriter;
        protected Supplier<IndexWriterConfig> configProvider;

        public LazyWriterProvider(LazyProxy<? extends Directory> lazyDirectory, Supplier<IndexWriterConfig> confProvider) {
            lazyDir = Objects.requireNonNull(lazyDirectory);
            configProvider = Objects.requireNonNull(confProvider);
            lazyIndexWriter = lazyDir.map((t, dir) -> updateIndexWriter(configProvider.get(), dir, t));

        }

        @Override
        public IndexWriter get() {
            return lazyIndexWriter.get();
        }

    }
}
