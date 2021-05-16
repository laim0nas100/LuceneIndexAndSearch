package lt.lb.lucenejpa.io.bb;

import java.io.IOException;
import java.util.Objects;
import lt.lb.lucenejpa.LastModifiedAware;
import lt.lb.lucenejpa.SyncDirectory;

/**
 *
 * @author laim0nas100
 */
public class SyncFlushDir extends SyncDirectory<FlushByteBuffersDirectory> {

    public SyncFlushDir(FlushByteBuffersDirectory dir) {
        this(dir, false);
    }

    public SyncFlushDir(FlushByteBuffersDirectory dir, boolean readOnly) {
        this.dir = Objects.requireNonNull(dir);
        this.readOnly = readOnly;
    }

    protected boolean readOnly;
    protected FlushByteBuffersDirectory dir;

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    protected FlushByteBuffersDirectory delegate() {
        return dir;
    }

    @Override
    protected LastModifiedAware getModifyAware() {
        return dir;
    }

    @Override
    public void syncRemote() throws IOException {
        if (isReadOnly()) {
            return;//no op
        }
        LuceneSync.syncRemote(dir);
    }

    @Override
    public void syncLocal() throws IOException {
        if (isReadOnly()) {
            return;//no op
        }
        LuceneSync.syncLocal(dir);
    }

}
