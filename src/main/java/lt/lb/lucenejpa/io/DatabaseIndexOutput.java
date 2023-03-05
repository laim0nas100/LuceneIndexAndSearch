package lt.lb.lucenejpa.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import lt.lb.luceneindexandsearch.splitting.DirConfig;
import lt.lb.luceneindexandsearch.splitting.JpaDirConfig;
import lt.lb.lucenejpa.Q;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.OutputStreamIndexOutput;

/**
 * An <code>IndexOutput</code> implementation that initially writes the data to
 * a memory buffer. Once it exceeds the configured threshold (
 * {@link DatabaseConfig#setThreshold(long)}, will start working with a
 * temporary file, releasing the previous buffer.
 *
 */
public class DatabaseIndexOutput extends OutputStreamIndexOutput {

    private static final Logger LOGGER = LogManager.getLogger(DatabaseIndexOutput.class);

    protected final String name;
    protected final JpaDirConfig config;
    @SuppressWarnings("unused")
    protected final IOContext context;

    protected ByteArrayOutputStream baos;

    protected long pos = 0;

    public DatabaseIndexOutput(ByteArrayOutputStream output, final JpaDirConfig directory, final String name, final IOContext context) {
        super(directory.getFolderName() + "/" + name, name, output, 1024);
        this.config = directory;
        this.name = name;
        this.context = context;
        baos = output;
    }

    public DatabaseIndexOutput(final JpaDirConfig directory, final String name, final IOContext context) {
        this(new ByteArrayOutputStream(), directory, name, context);
    }

    @Override
    public void close() throws IOException {
        LOGGER.trace("{}.close()", this);
        save();
    }

    public void save() throws IOException {
        Q.saveFileBytes(config, name, getContent().readAllBytes(), false, new Date());
    }

    public InputStream savedContent;

    public InputStream getContent() throws IOException {
        if (savedContent != null) {
            return savedContent;
        }
        if (baos == null) {
            return null;
        }
        savedContent = new ByteArrayInputStream(baos.toByteArray());

        return savedContent;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass().getSimpleName()).append(":").append(config).append("/")
                .append(name).toString();
    }
}
