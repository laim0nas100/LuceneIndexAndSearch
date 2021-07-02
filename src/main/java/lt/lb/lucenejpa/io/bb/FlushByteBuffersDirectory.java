package lt.lb.lucenejpa.io.bb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lt.lb.commons.Java;
import lt.lb.uncheckedutils.SafeOpt;
import lt.lb.commons.misc.compare.Compare;
import lt.lb.lucenejpa.DirConfig;
import lt.lb.lucenejpa.LastModifiedAware;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.BitUtil;

/**
 * A {@link ByteBuffer}-based {@link Directory} implementation that can be used
 * to store index files on the heap.
 *
 * <p>
 * Important: Note that {@link MMapDirectory} is nearly always a better choice
 * as it uses OS caches more effectively (through memory-mapped buffers). A
 * heap-based directory like this one can have the advantage in case of
 * ephemeral, small, short-lived indexes when disk syncs provide an additional
 * overhead.</p>
 *
 * @lucene.experimental
 */
public class FlushByteBuffersDirectory extends BaseDirectory implements LastModifiedAware {

    private static Logger logger = LogManager.getLogger(FlushByteBuffersDirectory.class);
    public static final BiFunction<String, FlushByteBuffersDataOutput, IndexInput> OUTPUT_AS_MANY_BUFFERS
            = (fileName, output) -> {
                FlushByteBuffersDataInput dataInput = output.toDataInput();
                String inputName = String.format(Locale.ROOT, "%s (file=%s, buffers=%s)",
                        FlushByteBuffersIndexInput.class.getSimpleName(),
                        fileName,
                        dataInput.toString());
                return new FlushByteBuffersIndexInput(dataInput, inputName);
            };

    public static final BiFunction<String, FlushByteBuffersDataOutput, IndexInput> OUTPUT_AS_ONE_BUFFER
            = (fileName, output) -> {
                FlushByteBuffersDataInput dataInput = new FlushByteBuffersDataInput(Arrays.asList(ByteBuffer.wrap(output.toArrayCopy())));
                String inputName = String.format(Locale.ROOT, "%s (file=%s, buffers=%s)",
                        FlushByteBuffersDataInput.class.getSimpleName(),
                        fileName,
                        dataInput.toString());
                return new FlushByteBuffersIndexInput(dataInput, inputName);
            };

    public static final BiFunction<String, FlushByteBuffersDataOutput, IndexInput> OUTPUT_AS_BYTE_ARRAY = OUTPUT_AS_ONE_BUFFER;

    public static final BiFunction<String, FlushByteBuffersDataOutput, IndexInput> OUTPUT_AS_MANY_BUFFERS_LUCENE
            = (fileName, output) -> {
                List<ByteBuffer> bufferList = output.toBufferList();
                bufferList.add(ByteBuffer.allocate(0));

                int chunkSizePower;
                int blockSize = FlushByteBuffersDataInput.determineBlockPage(bufferList);
                if (blockSize == 0) {
                    chunkSizePower = 30;
                } else {
                    chunkSizePower = Integer.numberOfTrailingZeros(BitUtil.nextHighestPowerOfTwo(blockSize));
                }

                String inputName = String.format(Locale.ROOT, "%s (file=%s)",
                        FlushByteBuffersDirectory.class.getSimpleName(),
                        fileName);

                MyByteBufferGuard guard = new MyByteBufferGuard("none", (String resourceDescription, ByteBuffer b) -> {
                });
                return MyByteBufferIndexInput.newInstance(inputName,
                        bufferList.toArray(new ByteBuffer[bufferList.size()]),
                        output.size(), chunkSizePower, guard);
            };

    private final Function<String, String> tempFileName = new Function<String, String>() {
        private final AtomicLong counter = new AtomicLong();

        @Override
        public String apply(String suffix) {
            return suffix + "_" + Java.getCurrentTimeMillis() + "_" + Long.toString(counter.getAndIncrement(), Character.MAX_RADIX);
        }
    };

    private final ConcurrentHashMap<String, FlushBBFileEntry> files = new ConcurrentHashMap<>();
    /**
     * Conversion between a buffered index output and the corresponding index
     * input for a given file.
     */
    private final BiFunction<String, FlushByteBuffersDataOutput, IndexInput> outputToInput;

    /**
     * A supplier of {@link ByteBuffersDataOutput} instances used to buffer up
     * the content of written files.
     */
    private final Supplier<FlushByteBuffersDataOutput> bbOutputSupplier;

    public final DirConfig dirConfig;

    private volatile Date lastChange;

    public FlushByteBuffersDirectory(DirConfig dirConfig) {
        this(dirConfig, new SingleInstanceLockFactory());
    }

    public FlushByteBuffersDirectory(DirConfig dirConfig, LockFactory lockFactory) {
        this(dirConfig, lockFactory, FlushByteBuffersDataOutput::new, OUTPUT_AS_MANY_BUFFERS);
    }

    public FlushByteBuffersDirectory(DirConfig dirConfig,
            LockFactory factory,
            Supplier<FlushByteBuffersDataOutput> bbOutputSupplier,
            BiFunction<String, FlushByteBuffersDataOutput, IndexInput> outputToInput) {
        super(factory);
        this.dirConfig = Objects.requireNonNull(dirConfig);
        this.outputToInput = Objects.requireNonNull(outputToInput);
        this.bbOutputSupplier = Objects.requireNonNull(bbOutputSupplier);
    }

    @Override
    public String[] listAll() throws IOException {
        ensureOpen();
        return files.keySet().stream().sorted().toArray(String[]::new);
    }

    @Override
    public void deleteFile(String name) throws IOException {
        logger.trace("deleteFile  {}", name);
        ensureOpen();
        FlushBBFileEntry removed = files.remove(name);
        if (removed == null) {
            throw new NoSuchFileException(name);
        }

    }

    @Override
    public long fileLength(String name) throws IOException {
        logger.trace("fileLength  {}", name);
        ensureOpen();
        FlushBBFileEntry file = files.get(name);
        if (file == null) {
            throw new NoSuchFileException(name);
        }
        return file.length();
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        logger.trace("createOutput  {} {}", name, context);
        ensureOpen();

        boolean temp = false;
        boolean lazy = false;
        FlushBBFileEntry e = new FlushBBFileEntry(dirConfig, bbOutputSupplier, name, temp, lazy);
        if (files.putIfAbsent(name, e) != null) {
            throw new FileAlreadyExistsException("File already exists: " + name);
        }
       
        return e.createOutput(outputToInput);
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        logger.trace("createTempOutput  {} {} {}", prefix, suffix, context);
        ensureOpen();

        while (true) {
            String name = IndexFileNames.segmentFileName(prefix, tempFileName.apply(suffix), "tmp");
            boolean temp = true;
            boolean lazy = false;
            FlushBBFileEntry e = new FlushBBFileEntry(dirConfig, bbOutputSupplier, name, temp, lazy);
            if (files.putIfAbsent(name, e) == null) {
                return e.createOutput(outputToInput);
            }
        }
    }

    public List<FlushBBFileEntry> getAllEntries() {
        return new ArrayList<>(files.values());
    }

    public void createLazyOutput(String name) throws IOException {
        logger.trace("createLazyOutput  {}", name);
        ensureOpen();

        boolean temp = false;
        boolean lazy = true;
        FlushBBFileEntry e = new FlushBBFileEntry(dirConfig, bbOutputSupplier, name, temp, lazy);
        if (files.putIfAbsent(name, e) != null) {
            throw new FileAlreadyExistsException("File already exists: " + name);
        }
        registerChange(name);
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        logger.trace("rename  {} {}", source, dest);
        ensureOpen();

        FlushBBFileEntry file = files.get(source);
        if (file == null) {
            throw new NoSuchFileException(source);
        }
        if (files.putIfAbsent(dest, file.rename(dest)) != null) {
            throw new FileAlreadyExistsException(dest);
        }
        if (!files.remove(source, file)) {
            throw new IllegalStateException("File was unexpectedly replaced: " + source);
        }
        files.remove(source);
        registerChange(dest);
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        ensureOpen();
    }

    @Override
    public void syncMetaData() throws IOException {
        ensureOpen();
    }

    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        logger.trace("openInput  {} {}", name, context);
        ensureOpen();

        FlushBBFileEntry e = files.get(name);
        if (e == null) {
            throw new NoSuchFileException(name);
        } else {
            return e.openInput();
        }
    }

    @Override
    public void close() throws IOException {
        logger.trace("close");
        isOpen = false;
        files.clear();
    }

    @Override
    public Set<String> getPendingDeletions() {
        return Collections.emptySet();
    }

    @Override
    public void registerChange(String name, final Date date) {
        if (Compare.compareNullLower(lastChange, Compare.CompareOperator.LESS, date)) { // accept only newer
            this.lastChange = date;
        }
        this.files.compute(name, (n, f) -> {
            if (f != null) {
                f.lastChange = date;
                return f;
            }
            return null;
        });
    }

    @Override
    public Date getLastChange() {
        return this.lastChange;
    }

    @Override
    public Date getLastChange(String name) {
        return SafeOpt.ofNullable(files.getOrDefault(name, null)).map(m -> m.lastChange).orNull();
    }

    @Override
    public void removeChange(String name) {//removed by something else
        this.lastChange = new Date();
    }

}
