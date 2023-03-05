package lt.lb.lucenejpa.io;

import java.io.IOException;
import java.util.Date;
import lt.lb.luceneindexandsearch.splitting.DirConfig;
import lt.lb.luceneindexandsearch.splitting.JpaDirConfig;
import lt.lb.lucenejpa.Q;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.LockObtainFailedException;

public class DatabaseReadWriteLockFactory extends LockFactory {

    private static final Logger LOGGER = LogManager.getLogger(DatabaseReadWriteLockFactory.class);

    protected JpaDirConfig config;

    public DatabaseReadWriteLockFactory(JpaDirConfig conf) {
        this.config = conf;
    }

    @Override
    public Lock obtainLock(final Directory dir, final String lockName) throws IOException {
        LOGGER.info("{}.obtainLock({}, {})", this, dir, lockName);
        if (Q.existsFile(config, lockName)) {
            throw new LockObtainFailedException("Lock instance already obtained: " + lockName + " at dir:" + config.getConfigID());
        }
        Q.saveFile(config, lockName, null, 0, true, new Date());
        return new DatabaseReadWriteLock(config, lockName);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

}
