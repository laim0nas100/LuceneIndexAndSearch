package lt.lb.lucenejpa.io.bb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import lt.lb.lucenejpa.DirConfig;
import lt.lb.lucenejpa.Q;
import lt.lb.uncheckedutils.SafeOpt;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

/**
 *
 * @author laim0nas100
 */
public class FlushByteBuffersDirectoryReadOnly extends FlushByteBuffersDirectory {

    public FlushByteBuffersDirectoryReadOnly(DirConfig dirConfig) {
        super(dirConfig);
    }

    @Override
    public Date getLastChange(String name) {
        return SafeOpt.ofGet(() -> {
            return Q.fileLastMofified(dirConfig, name).orElse(null);
        }).orElse(null);
    }

    @Override
    public Date getLastChange() {
        return SafeOpt.ofGet(() -> {
            return Q.fileDirectoryLastMofified(dirConfig).orElse(null);
        }).orElse(null);
    }

    @Override
    public void registerChange(String name, Date date) {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        byte[] bytes = Q.fileContentBytes(this.dirConfig, name).orElseThrow(FileNotFoundException::new);
        return LuceneSync.indexInput(bytes, name);
//        return new SimpleIndexInput(bytes, name, IOContext.READ);

    }

    @Override
    public void rename(String source, String dest) throws IOException {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public void createLazyOutput(String name) throws IOException {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public List<FlushBBFileEntry> getAllEntries() {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public long fileLength(String name) throws IOException {
        return Q.fileLength(dirConfig, name);
    }

    @Override
    public void deleteFile(String name) throws IOException {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public String[] listAll() throws IOException {
        return Q.listAll(dirConfig);
    }

    @Override
    public void copyFrom(Directory from, String src, String dest, IOContext context) throws IOException {
        throw new UnsupportedOperationException("Read only directory");
    }

    @Override
    public ChecksumIndexInput openChecksumInput(String name, IOContext context) throws IOException {
        return super.openChecksumInput(name, context); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    

}
