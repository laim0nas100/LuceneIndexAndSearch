package lt.lb.lucenejpa.io.bb;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lt.lb.luceneindexandsearch.splitting.DirConfig;
import lt.lb.luceneindexandsearch.splitting.JpaDirConfig;
import lt.lb.lucenejpa.Q;
import lt.lb.lucenejpa.model.LuceneFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

/**
 *
 * @author laim0nas100
 */
public class LuceneSync {

    public static void syncLocal(FlushByteBuffersDirectory localDir) throws IOException {
        JpaDirConfig config = localDir.dirConfig;

        List<LuceneFile> remoteFile = Q.listAllFiles(config);
        Map<String, Date> localMap = new HashMap<>();
        Map<String, LuceneFile> remoteMap = new HashMap<>();
        String[] allLocal = localDir.listAll();
        for (LuceneFile luceneFile : remoteFile) {
            String name = luceneFile.getFileName();
            remoteMap.put(name, luceneFile);
        }

        for (String name : allLocal) {
            localMap.put(name, localDir.getLastChange(name));
            if (!remoteMap.containsKey(name)) {
                localDir.deleteFile(name);
            }
        }

        for (LuceneFile luceneFile : remoteFile) {
            String name = luceneFile.getFileName();
            boolean toWrite = true;
            if (localMap.containsKey(name)) {
                Date lastModiDate = localMap.get(name);
                if (dateEq(luceneFile.getLastModified(), lastModiDate)) {
                    toWrite = false;
                }
            }
            if (toWrite) {
                writeLocal(localDir, luceneFile);
            }
        }
    }

    static final long smallEnoughSize = 16 * 1024;
    // under limit, so just keep in memory, 
    // hopefully Lucene does not spam small files and it does not overflow.

    private static void writeLocal(FlushByteBuffersDirectory localDir, LuceneFile luceneFile) throws IOException {
        String name = luceneFile.getFileName();
        long size = luceneFile.getFileSize();
        if (size <= smallEnoughSize) {
            try (IndexOutput localOutput = localDir.createOutput(name, IOContext.DEFAULT)) {
                byte[] content = luceneFile.getLuceneBlob().getContent();
                localOutput.writeBytes(content, (int) size);
            }
        } else {
            localDir.createLazyOutput(name);
        }
        localDir.registerChange(name, luceneFile.getLastModified());

    }

    public static void syncRemote(FlushByteBuffersDirectory localDir) throws IOException {
        JpaDirConfig config = localDir.dirConfig;
        List<FlushBBFileEntry> makeLazy = new ArrayList<>();
        List<LuceneFile> remoteFile = Q.listAllFiles(config);
        Map<String, LuceneFile> remoteMap = new HashMap<>();
        Map<String, FlushBBFileEntry> localMap = new HashMap<>();
        List<FlushBBFileEntry> allLocal = localDir.getAllEntries();
        List<LuceneFile> toDelete = new ArrayList<>();
        List<FlushBBFileEntry> toSave = new ArrayList<>();

        for (FlushBBFileEntry local : allLocal) {
            localMap.put(local.fileName, local);
        }
        for (LuceneFile file : remoteFile) {
            remoteMap.put(file.getFileName(), file);
            if (!localMap.containsKey(file.getFileName())) {
                toDelete.add(file);
            }
        }

        for (FlushBBFileEntry local : allLocal) {
            String name = local.fileName;
            Date lastModiDate = local.lastChange;
            if (remoteMap.containsKey(name)) {
                LuceneFile luceneFile = remoteMap.get(name);
                if (dateDiff(luceneFile.getLastModified(), lastModiDate)) {
                    toSave.add(local);
                }

            } else {
                toSave.add(local);
            }
        }

        for (LuceneFile deletable : toDelete) {
            config.getEntityManager().remove(deletable);
        }
        for (FlushBBFileEntry savable : toSave) {
            if (savable.isLazy()) {
                throw new IllegalArgumentException("Trying to save lazy file entry " + savable.fileName);
            }
            long size = savable.length();
            Q.saveFileBytes(config, savable.fileName, toBytes(savable.openInput(), (int) size), savable.temp, savable.lastChange);
            if (size > smallEnoughSize) {
                makeLazy.add(savable);
            }
        }

        for (FlushBBFileEntry lazify : makeLazy) {
            lazify.makeLazy();
        }
    }

    public static boolean dateDiff(Date date1, Date date2) {
        return !dateEq(date1, date2);
    }

    public static boolean dateEq(Date date1, Date date2) {
        if (date1 == date2) {
            return true;
        }
        if (date1 == null || date2 == null) {
            return false;
        }

        return date1.getTime() == date2.getTime();
    }

    public static byte[] toBytes(DataInput input, int length) throws IOException {
        byte[] bytes = new byte[length];
        input.readBytes(bytes, 0, length, false);
        return bytes;
    }

    public static IndexInput indexInput(byte[] bytes, String desc){
        Objects.requireNonNull(bytes);
        return new FlushByteBuffersIndexInput(new FlushByteBuffersDataInput(Arrays.asList(ByteBuffer.wrap(bytes))), desc);
    }

}
