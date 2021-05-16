package lt.lb.lucenejpa;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;

/**
 *
 * @author laim0nas100
 */
public abstract class SyncDirectory<T extends Directory> extends ForwardingDirectory<T> implements LastModifiedAware {

    public boolean isReadOnly() {
        return false;
    }
    
    public boolean isEmpty() throws IOException{
        return listAll().length == 0;
    }

    @Override
    protected abstract T delegate();

    protected abstract LastModifiedAware getModifyAware();

    @Override
    public Date getLastChange() {
        return getModifyAware().getLastChange();
    }

    @Override
    public void registerChange(String name, Date date) {
        getModifyAware().registerChange(name, date);
    }

    @Override
    public Date getLastChange(String name) {
        return getModifyAware().getLastChange(name);
    }

    @Override
    public void removeChange(String name) {
        getModifyAware().removeChange(name);
    }

    /**
     * Commit changes to the remote repository from local For example memory ->
     * database
     *
     * @throws IOException
     */
    public abstract void syncRemote() throws IOException;

    /**
     * Commit changes to local from remote repository
     *
     * For example database -> memory
     *
     * @throws IOException
     */
    public abstract void syncLocal() throws IOException;

    protected void assertNotReadOnly() {
        if (isReadOnly()) {
            throw new UnsupportedOperationException("Directory is read only, no change is allowed");
        }
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        assertNotReadOnly();
        super.rename(source, dest);
        removeChange(source);
        registerChange(dest);
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        IndexOutput tempOutput = super.createTempOutput(prefix, suffix, context);
        registerChange(tempOutput.getName());
        return tempOutput;
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        assertNotReadOnly();
        IndexOutput output = super.createOutput(name, context);
        registerChange(name);
        return output;
    }

    @Override
    public void deleteFile(String name) throws IOException {
        assertNotReadOnly();
        super.deleteFile(name);
        removeChange(name);
    }

    public static <T extends Directory> SyncDirectory<T> syncNoOp(final T dir, boolean readOnly) {
        Objects.requireNonNull(dir);
        LastModifiedAware nop = LastModifiedAware.ofNOP();
        return new SyncDirectory<T>() {
            @Override
            protected T delegate() {
                return dir;
            }

            @Override
            public boolean isReadOnly() {
                return readOnly;
            }

            @Override
            protected LastModifiedAware getModifyAware() {
                return nop;
            }

            @Override
            public void syncRemote() throws IOException {
            }

            @Override
            public void syncLocal() throws IOException {
            }

            @Override
            public void registerChange(String name) {
            }

        };
    }

}
