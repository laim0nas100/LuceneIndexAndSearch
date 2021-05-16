/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.lucenejpa.io.bb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import lt.lb.lucenejpa.DirConfig;
import lt.lb.lucenejpa.Q;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

/**
 *
 * @author laim0nas100
 */
public class FlushBBFileEntry {

    public final String fileName;

    private volatile IndexInput content;
    private volatile boolean lazy = false;
    private volatile long cachedLength;
    private Supplier<FlushByteBuffersDataOutput> bbOutputSupplier;
    public final boolean temp;

    private final DirConfig conf;
    public volatile Date lastChange = new Date();

    public FlushBBFileEntry(DirConfig conf, Supplier<FlushByteBuffersDataOutput> bbSupplier, String name, boolean temp, boolean lazy) {
        this.fileName = name;
        this.temp = temp;
        this.conf = Objects.requireNonNull(conf);
        this.bbOutputSupplier = Objects.requireNonNull(bbSupplier);
        this.lazy = lazy;
        if (lazy) {
            cachedLength = -1;
        }
    }

    public FlushBBFileEntry rename(String newName) {
        FlushBBFileEntry renamed = new FlushBBFileEntry(conf, bbOutputSupplier, newName, temp, lazy);
        renamed.cachedLength = this.cachedLength;
        renamed.content = this.content;
        return renamed;
    }

    public long length() throws IOException {
        if (lazy && cachedLength < 0) {
            cachedLength = Q.fileLength(conf, fileName);
        }
        // We return 0 length until the IndexOutput is closed and flushed.
        return cachedLength;
    }

    public IndexInput openInput() throws IOException {

        if (lazy) {
            byte[] bytes = Q.fileContentBytes(conf, fileName).orElseThrow(FileNotFoundException::new);
            return LuceneSync.indexInput(bytes, fileName);
//            return new SimpleIndexInput(bytes, fileName, IOContext.READ);
        }
        IndexInput local = this.content;
        if (local == null) {
            throw new AccessDeniedException("Can't open a file still open for writing: " + fileName);
        }

        return local.clone();
    }

    public void makeLazy() {
        if (lazy) {
            return;
        }
        this.content = null;

        lazy = true;
    }
    
    public boolean isLazy(){
        return lazy;
    }

    final IndexOutput createOutput(BiFunction<String, FlushByteBuffersDataOutput, IndexInput> outputToInput) throws IOException {
        if (content != null) {
            throw new IOException("Can only write to a file once: " + fileName);
        }
        if (lazy && content == null) {
            throw new IOException("Cannot open output lazily: " + fileName);
        }

        String clazzName = FlushByteBuffersDirectory.class.getSimpleName();
        String outputName = String.format(Locale.ROOT, "%s output (file=%s)", clazzName, fileName);

        return new FlushByteBuffersIndexOutput(
                bbOutputSupplier.get(), outputName, fileName,
                new CRC32(),
                (output) -> {
                    content = outputToInput.apply(fileName, output);
                    cachedLength = output.size();
                });
    }
}
