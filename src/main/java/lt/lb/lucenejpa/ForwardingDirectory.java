package lt.lb.lucenejpa;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;

/**
 *
 * @author laim0nas100
 */
public abstract class ForwardingDirectory<T extends Directory> extends Directory{

    protected abstract T delegate();
    
    @Override
    public String[] listAll() throws IOException {
        return delegate().listAll();
    }

    @Override
    public void deleteFile(String name) throws IOException {
        delegate().deleteFile(name);
    }

    @Override
    public long fileLength(String name) throws IOException {
        return delegate().fileLength(name);
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        return delegate().createOutput(name, context);
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        return delegate().createTempOutput(prefix, suffix, context);
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        delegate().sync(names);
    }

    @Override
    public void syncMetaData() throws IOException {
        delegate().syncMetaData();
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        delegate().rename(source, dest);
    }

    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        return delegate().openInput(name, context);
    }

    @Override
    public Lock obtainLock(String name) throws IOException {
        return delegate().obtainLock(name);
    }

    @Override
    public void close() throws IOException {
        delegate().close();
    }

    @Override
    public Set<String> getPendingDeletions() throws IOException {
        return delegate().getPendingDeletions();
    }
    
}
