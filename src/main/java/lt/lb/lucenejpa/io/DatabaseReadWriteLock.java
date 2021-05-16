package lt.lb.lucenejpa.io;

import java.io.IOException;
import lt.lb.lucenejpa.DirConfig;
import lt.lb.lucenejpa.Q;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Lock;

/**
 *
 * @author Lemmin
 */
public class DatabaseReadWriteLock extends Lock {

    private static final Logger LOGGER = LogManager.getLogger(DatabaseReadWriteLock.class);
    private final DirConfig config;
    private final String name;
    private volatile boolean closed;

    public DatabaseReadWriteLock(final DirConfig config, final String name) {
        this.config = config;
        this.name = name;
    }

    @Override
    public void ensureValid() throws IOException {
        LOGGER.debug("{}.ensureValid()", this);
        if (closed) {
            throw new AlreadyClosedException("Lock instance already released: " + this);
        }
        if (!Q.existsFile(config, name)) {
            throw new AlreadyClosedException("Lock instance already released: " + this);
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("{}.close()", this);
        if (!closed) {
            Q.deleteFile(config, name);
            closed = true;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
