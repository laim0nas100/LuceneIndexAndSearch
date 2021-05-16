package lt.lb.lucenejpa.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import lt.lb.commons.io.ExtInputStream;
import lt.lb.commons.io.ForwardingExtInputStream;
import lt.lb.lucenejpa.DirConfig;
import lt.lb.lucenejpa.Q;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.BufferedChecksum;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
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
    protected final DirConfig config;
    @SuppressWarnings("unused")
    protected final IOContext context;

    protected ByteArrayOutputStream baos;

    protected long pos = 0;
    public DatabaseIndexOutput(ByteArrayOutputStream output,final DirConfig directory, final String name, final IOContext context){
        super(directory.getFolderName() + "/" + name, name,output,1024);
        this.config = directory;
        this.name = name;
        this.context = context;
        baos = output;
    }
    
    public DatabaseIndexOutput(final DirConfig directory, final String name, final IOContext context) {
       this(new ByteArrayOutputStream(),directory,name,context);
    }

//    @Override
//    public long getFilePointer() {
//        LOGGER.trace("{}.getFilePointer()", this);
//        return pos;
//    }
//
//    @Override
//    public long getChecksum() throws IOException {
//        LOGGER.trace("{}.getChecksum()", this);
//        return digest.getValue();
//    }
//
//    @Override
//    public void writeByte(final byte b) throws IOException {
//        LOGGER.trace("{}.writeByte({})", this, b);
//        write(new byte[]{b}, 0, 1);
//    }
//
//    @Override
//    public void writeBytes(final byte[] b, final int offset, final int length) throws IOException {
//        LOGGER.trace("{}.writeBytes({}, {}, {})", this, b, offset, length);
//        write(b, offset, length);
//    }

//    public void write(final byte[] b, final int offset, final int length) throws IOException {
//        pos += length;
//        if (file == null && config.getThreshold() > pos) {
//            baos.write(b, offset, length);
//        } else {
//            if (file == null) {
//                tempFile = File.createTempFile(
//                        config.tempFileName(name), ".ljt");
//                file = new RandomAccessFile(tempFile, "rw");
//                file.write(baos.toByteArray(), 0, baos.size());
//                baos = null;
//            }
//            file.write(b, offset, length);
//        }
//        digest.update(b, offset, length);
//    }

    @Override
    public void close() throws IOException {
        LOGGER.trace("{}.close()", this);
        save();
    }

    public void save() throws IOException {
        Q.saveFileBytes(config, name, getContent().readAllBytes(), false, new Date());
    }
    
    public ExtInputStream savedContent;

    public ExtInputStream getContent() throws IOException {
        if(savedContent != null){
            return savedContent;
        }
            if(baos == null){
                return null;
            }
            savedContent =  ForwardingExtInputStream.of(new ByteArrayInputStream(baos.toByteArray()));
        
        return savedContent;
    }


    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass().getSimpleName()).append(":").append(config).append("/")
                .append(name).toString();
    }
}
