package lt.lb.lucenejpa;

import lt.lb.luceneindexandsearch.splitting.KindConfig;
import lt.lb.luceneindexandsearch.splitting.JpaDirConfig;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import lt.lb.commons.io.stream.ForwardingInputStream;
import static lt.lb.commons.jpa.querydecor.JpaDecorHelp.*;
import lt.lb.commons.jpa.querydecor.JpaQueryDecor;
import lt.lb.lucenejpa.model.LuceneBlob;
import lt.lb.lucenejpa.model.LuceneFile;
import lt.lb.lucenejpa.model.LuceneFile_;
import lt.lb.uncheckedutils.SafeOpt;
import lt.lb.uncheckedutils.func.UncheckedConsumer;
import lt.lb.uncheckedutils.func.UncheckedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author laim0nas100
 */
public class Q {

    private static final Logger LOGGER = LogManager.getLogger(Q.class);

    private static JpaQueryDecor<LuceneFile, LuceneFile> baseKindDecor(JpaDirConfig conf) {
        Objects.requireNonNull(conf);
        return JpaQueryDecor.of(LuceneFile.class)
                .withPred(equal(LuceneFile_.fileOrigin, conf.getFileOrigin()))
                .withPred(equal(LuceneFile_.fileKind, conf.getFileKind()));
    }

    private static JpaQueryDecor<LuceneFile, LuceneFile> baseDecor(JpaDirConfig conf) {
        Objects.requireNonNull(conf);
        return baseKindDecor(conf)
                .withPred(equal(LuceneFile_.folderName, conf.getFolderName()))
                .setLockMode(LockModeType.NONE);
    }

    private static JpaQueryDecor<LuceneFile, LuceneFile> baseDecor(JpaDirConfig conf, String fileName) {
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return baseDecor(conf)
                .withPred(equal(LuceneFile_.fileName, fileName))
                .setLockMode(LockModeType.NONE);
    }

    public static class IdName {

        public final long id;
        public final String name;

        public IdName(long id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    public static void transactionRun(JpaDirConfig conf, UncheckedConsumer<EntityManager> run) throws IOException {

        conf.getLuceneExecutor().execute(() -> {
            run.accept(conf.getEntityManager());
        }).peekError(err -> {
            LOGGER.error("Q error", err);
        }).throwIfErrorUnwrapping(IOException.class);
    }

    public static <T> T transactionCall(JpaDirConfig conf, UncheckedFunction<EntityManager, T> call) throws IOException {
        return conf.getLuceneExecutor().call(() -> {

            return call.apply(conf.getEntityManager());
        }).peekError(err -> {
            LOGGER.error("Q error", err);
        }).throwIfErrorUnwrapping(IOException.class).orNull();
    }

    public static List<LuceneFile> listAllFiles(JpaDirConfig conf) throws IOException {
        LOGGER.trace("listAll({})", conf);
        return transactionCall(conf, em -> {
            return baseDecor(conf)
                    .buildList(em);
        });
    }

    public static IdName[] listAllIdsName(JpaDirConfig conf) throws IOException {
        LOGGER.trace("listAllIdsName({})", conf);
        return transactionCall(conf, em -> {

            return baseDecor(conf)
                    .buildStream(em)
                    .map(file -> new IdName(file.getId(), file.getFileName()))
                    .toArray(s -> new IdName[s]);
        });
    }

    public static String[] listAll(JpaDirConfig conf) throws IOException {
        LOGGER.trace("listAll({})", conf);
        return transactionCall(conf, em -> {
            return baseDecor(conf)
                    .selecting(LuceneFile_.fileName)
                    .withPred(isFalse(LuceneFile_.temp))
                    .buildStream(em)
                    .sorted()
                    .toArray(s -> new String[s]);
        });
    }

    public static String[] listDistinctFolders(JpaDirConfig conf) throws IOException {
        LOGGER.trace("listDistinctFolders({})", conf);
        return transactionCall(conf, em -> {
            return baseKindDecor(conf)
                    .selecting(LuceneFile_.folderName)
                    .setDistinct(true)
                    .buildStream(em)
                    .sorted()
                    .toArray(s -> new String[s]);
        });
    }

    public static Set<String> listTemp(JpaDirConfig conf) throws IOException {
        LOGGER.trace("listTemp({})", conf);
        return transactionCall(conf, em -> {
            return baseDecor(conf)
                    .selecting(LuceneFile_.fileName)
                    .withPred(isTrue(LuceneFile_.temp))
                    .buildStream(em)
                    .collect(Collectors.toSet());
        });
    }

    public static SafeOpt<Long> fileId(JpaDirConfig conf, String fileName) throws IOException {
        LOGGER.trace("listTemp({},{})", conf, fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .selecting(LuceneFile_.id)
                    .buildUniqueResult(em)
                    .throwIfErrorAsNested();
        });

    }

    public static boolean deleteFile(JpaDirConfig conf, String fileName) throws IOException {
        LOGGER.trace("deleteFile({},{})", conf, fileName);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .buildUniqueResult(em)
                    .map(file -> {
                        LOGGER.trace("deleting({},{})", conf, fileName);
                        em.remove(file);
                        return true;
                    })
                    .throwIfErrorAsNested()
                    .orElse(false);

        });
    }

    public static boolean existsFile(JpaDirConfig conf, String fileName) throws IOException {
        LOGGER.trace("existsFile({},{})", conf, fileName);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .buildUniqueResult(em)
                    .throwIfErrorAsNested()
                    .isPresent();
        });

    }

    public static boolean renameFile(JpaDirConfig conf, String src, String dst) throws IOException {
        LOGGER.trace("renameFile({},{},{})", conf, src, dst);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(src);
        Objects.requireNonNull(dst);
        return transactionCall(conf, em -> {
            SafeOpt<LuceneFile> file = baseDecor(conf, src)
                    .buildUniqueResult(em).throwIfErrorAsNested();

            if (file.isEmpty()) {
                return false; // failed to rename
            } else {
                LuceneFile get = file.get();
                get.setFileName(dst);
                if (get.getId() == null) {
                    em.persist(get);
                } else {
                    em.merge(get);
                }
                return true;
            }
        });

    }

    public static long fileLength(JpaDirConfig conf, String fileName) throws IOException {
        LOGGER.trace("fileLength({},{})", conf, fileName);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .selecting(LuceneFile_.fileSize)
                    .buildUniqueResult(em)
                    .throwIfErrorAsNested()
                    .orElseThrow(() -> new FileNotFoundException(conf.getFolderName() + "/" + fileName));
        });
    }

    public static SafeOpt<Date> fileLastMofified(JpaDirConfig conf, String fileName) throws IOException {
        LOGGER.trace("fileLastMofified({},{})", conf, fileName);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .selecting(LuceneFile_.lastModified)
                    .buildUniqueResult(em)
                    .throwIfErrorAsNested();
        });
    }

    public static SafeOpt<Date> fileDirectoryLastMofified(JpaDirConfig conf) throws IOException {
        LOGGER.trace("fileDirectoryLastMofified({)", conf);
        Objects.requireNonNull(conf);
        return transactionCall(conf, em -> {
            return baseDecor(conf)
                    .selecting(LuceneFile_.lastModified)
                    .setOrderByDesc(LuceneFile_.lastModified)
                    .setMaxResults(1)
                    .buildUniqueResult(em)
                    .throwIfErrorAsNested();
        });
    }

    public static SafeOpt<InputStream> fileContent(JpaDirConfig conf, String fileName) throws IOException {
        LOGGER.trace("fileContent({},{})", conf, fileName);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .selecting(LuceneFile_.luceneBlob)
                    .buildUniqueResult(em)
                    .throwIfErrorAsNested()
                    .map(blob -> {
                        if (conf.bufferedJPAStreams()) {
                            return new ByteArrayInputStream(blob.getContent());
//                            return blob.getBinaryStream(); // no change
                        } else {
                            return new ByteArrayInputStream(blob.getContent());
//                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) blob.length());
//                            IOUtils.copy(blob.getBinaryStream(), byteArrayOutputStream);
//                            blob.free();
//                            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                        }

                    });
        });
    }

    public static SafeOpt<byte[]> fileContentBytes(JpaDirConfig conf, String fileName) throws IOException {
        LOGGER.trace("fileContent({},{})", conf, fileName);
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .selecting(LuceneFile_.luceneBlob)
                    .buildUniqueResult(em)
                    .map(blob -> {
                        return blob.getContent();
                    })
                    .throwIfErrorAsNested();
        });
    }

    public static boolean saveFile(JpaDirConfig conf, String fileName, InputStream stream, long length, boolean temp, Date date) throws IOException {
        LOGGER.trace("saveFile({},{})", conf, fileName);

        return transactionCall(conf, em -> {
            SafeOpt<LuceneFile> currentFile = baseDecor(conf, fileName).buildUniqueResult(em);

            LuceneFile file = currentFile.throwIfErrorAsNested().orElseGet(LuceneFile::new);

            if (stream != null) {
                if (conf.bufferedJPAStreams()) {
                    throw new IllegalArgumentException("Streams not supported");
//                    file.setContent(BlobProxy.generateProxy(stream, length));
                } else {
                    byte[] bytes = stream.readAllBytes();
//                    file.setContent(BlobProxy.generateProxy(bytes));
                    if (file.getLuceneBlob() == null) {
                        file.setLuceneBlob(new LuceneBlob());
                    }
                    file.getLuceneBlob().setContent(bytes);
                    int len = (int) length;
                    if (len != bytes.length) {
                        throw new IllegalArgumentException("Length missmatch for file: " + fileName + " " + length + " and array:" + bytes.length);
                    }
                }

            } else {
                if (length != 0) {
                    throw new IllegalArgumentException("No content and size:" + length);
                }
            }
            file.setFileName(fileName);
            file.setFolderName(conf.getFolderName());
            file.setLastModified(date);
            file.setTemp(temp);
            file.setFileSize(length);
            file.setFileKind(conf.getFileKind());
            file.setFileOrigin(conf.getFileOrigin());
            em.persist(file);
//            em.flush();

            return true;
        });

    }

    public static boolean saveFileBytes(JpaDirConfig conf, String fileName, byte[] content, boolean temp, Date date) throws IOException {
        LOGGER.trace("saveFileBytes({},{})", conf, fileName);
        ByteArrayInputStream stream = new ByteArrayInputStream(content);
        ForwardingInputStream extInput = new ForwardingInputStream() {
            @Override
            public InputStream delegate() {
                return stream;
            }
        };

        return saveFile(conf, fileName, extInput, content.length, temp, date);
    }
}
