package lt.lb.lucenejpa.io;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import lt.lb.commons.io.ExtInputStream;
import lt.lb.commons.io.ForwardingExtInputStream;
import lt.lb.lucenejpa.DirConfig;
import lt.lb.lucenejpa.Q;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;

public class DatabaseIndexInput extends IndexInput {

    private static final Logger LOGGER = LogManager.getLogger(DatabaseIndexInput.class);

    private final DirConfig directory;
    private final String name;
    private ExtInputStream stream;
    private byte[] bytes;
    private long pos = 0;
//    ByteBuffer buffer;

    public DatabaseIndexInput(final DirConfig dir, final String name, final IOContext context) throws IOException {
        super(name);
        this.directory = dir;
        this.name = name;
        bytes = Q.fileContentBytes(dir, name).orElseThrow(FileNotFoundException::new);
        stream = ForwardingExtInputStream.of(new ByteArrayInputStream(bytes));
    }

    @Override
    public void close() throws IOException {
        LOGGER.trace("{}.close()", this);
        stream.close();
        stream = null;
    }

    @Override
    public long length() {
        LOGGER.trace("{}.length()", this);
        return bytes.length;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass().getSimpleName()).append(":").append(directory).append("/")
                .append(name).toString();
    }

    public ExtInputStream getStream() {
        return stream;
    }

    @Override
    public long getFilePointer() {
        return pos;
    }

    @Override
    public void seek(long pos) throws IOException {
        if (pos >= length()) {
            throw new EOFException("Seek:" + pos + " length:" + length());
        }
        this.pos = pos;
        stream.reset();
        stream.skip(pos);

    }

    @Override
    public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
        return BufferedIndexInput.wrap(sliceDescription, this, offset, length);

    }

    @Override
    public byte readByte() throws IOException {
        pos++;
        return (byte) stream.read();
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) throws IOException {
        pos += len;
        stream.read(b, offset, len);
    }
}
