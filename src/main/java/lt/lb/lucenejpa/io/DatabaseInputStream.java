package lt.lb.lucenejpa.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import lt.lb.commons.F;
import lt.lb.uncheckedutils.SafeOpt;
import lt.lb.commons.jpa.EntityFacade;
import lt.lb.uncheckedutils.Checked;
import lt.lb.uncheckedutils.func.UncheckedSupplier;

/**
 *
 * @author laim0nas100
 */
public class DatabaseInputStream extends InputStream {

    public InputStream real;

    long pos = 0;
    public final UncheckedSupplier<InputStream> streamSupply;
    public final Supplier<EntityFacade> emSupply;

    public DatabaseInputStream(Supplier<EntityFacade> emSupply, UncheckedSupplier<InputStream> streamSupply) {
        this.streamSupply = streamSupply;
        real = getNewStream();
        this.emSupply = emSupply;
    }

    private InputStream getNewStream() {
        return SafeOpt.ofGet(streamSupply).orElse(new ByteArrayInputStream(new byte[0]));
    }

    @Override
    public int read() throws IOException {
        int read = real.read();
        pos++;
        return read;
    }

    @Override
    public void close() throws IOException {
        EntityFacade ef = emSupply.get();
        Checked.uncheckedRun(() -> {
            ef.executeTransactionAsync(em -> {
                real.close();
            }).get();
        });

    }

    private long mark = -1;

    @Override
    public synchronized void mark(int readlimit) {
        if (real.markSupported()) {
            real.mark(0);
        } else {
            mark = pos;
        }
    }

    @Override
    public synchronized void reset() throws IOException {

        try {
            if (real.markSupported()) {
                real.reset();
            } else {
                resetStream();
            }
        } catch (IOException ex) {
            resetStream();
        }

    }

    protected void resetStream() throws IOException {
        real.close();
        real = getNewStream();
        if (mark > 0) {
            real.skip(mark);
            pos = mark;
            mark = -1;
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

}
