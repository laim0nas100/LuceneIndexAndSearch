package lt.lb.lucenejpa.io;

import java.io.IOException;
import java.util.Date;
import lt.lb.luceneindexandsearch.splitting.JpaDirConfig;
import lt.lb.lucenejpa.Q;
import org.apache.lucene.store.IOContext;

/**
 *
 * @author laim0nas100
 */
public class DatabateIndexTempOutput extends DatabaseIndexOutput {

    public DatabateIndexTempOutput(JpaDirConfig directory, String name, IOContext context) {
        super(directory, name, context);
    }

    @Override
    public void save() throws IOException {
        Q.saveFileBytes(config, name, getContent().readAllBytes(), true, new Date());
    }

}
