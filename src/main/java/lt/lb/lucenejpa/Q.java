package lt.lb.lucenejpa;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lt.lb.uncheckedutils.SafeOpt;
import lt.lb.commons.io.ExtInputStream;
import lt.lb.commons.io.ForwardingExtInputStream;
import lt.lb.commons.jpa.querydecor.JpaQueryDecor;
import lt.lb.uncheckedutils.NestedException;
import lt.lb.lucenejpa.model.LuceneBlob;
import lt.lb.lucenejpa.model.LuceneFile;
import lt.lb.lucenejpa.model.LuceneFile_;
import lt.lb.uncheckedutils.func.UncheckedConsumer;
import lt.lb.uncheckedutils.func.UncheckedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Lemmin
 */
public class Q {

    private static final Logger LOGGER = LogManager.getLogger(Q.class);

    private static <T> List<Predicate> directorySelected(DirConfig conf, CriteriaBuilder builder, Path<LuceneFile> path) {
        List<Predicate> predicates = new ArrayList<>(1);
        Predicate equal = builder.equal(path.get(LuceneFile_.folderName), conf.getFolderName());
        predicates.add(equal);
        return predicates;
    }

    private static <T> CriteriaQuery<T> addPredicates(List<Predicate> pred, CriteriaQuery<T> query) {
        Predicate[] array = pred.stream().toArray(s -> new Predicate[s]);
        return query.where(array);
    }

    private static JpaQueryDecor<LuceneFile, LuceneFile> baseKindDecor(KindConfig conf) {
        Objects.requireNonNull(conf);
        return JpaQueryDecor.of(LuceneFile.class)
                .withPred(LuceneFile_.fileOrigin, (c, p) -> c.equal(p, conf.getFileOrigin()))
                .withPred(LuceneFile_.fileKind, (c, p) -> c.equal(p, conf.getFileKind()));
    }

    private static JpaQueryDecor<LuceneFile, LuceneFile> baseDecor(DirConfig conf) {
        Objects.requireNonNull(conf);
        return baseKindDecor(conf)
                .withPred(LuceneFile_.folderName, (c, p) -> c.equal(p, conf.getFolderName()));
    }

    private static JpaQueryDecor<LuceneFile, LuceneFile> baseDecor(DirConfig conf, String fileName) {
        Objects.requireNonNull(conf);
        Objects.requireNonNull(fileName);
        return baseDecor(conf)
                .withPred(LuceneFile_.fileName, (c, p) -> c.equal(p, fileName));
    }

    public static class IdName {

        public final long id;
        public final String name;

        public IdName(long id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    public static void transactionRun(KindConfig conf, UncheckedConsumer<EntityManager> run) throws IOException {
        try {

            if (conf.useAsync()) {
                conf.getEntityFacade().executeTransactionAsync(run).get(conf.getSecondsTimeout(), TimeUnit.SECONDS);
            } else {
                conf.getEntityFacade().executeTransaction(() -> {
                    run.accept(conf.getEntityManager());
                });
            }

//           
        } catch (Exception ex) {
            LOGGER.error("TX error", ex);
            throw NestedException.of(ex);
        }
    }

    public static <T> T transactionCall(KindConfig conf, UncheckedFunction<EntityManager, T> call) throws IOException {
        try {
            if (conf.useAsync()) {
                return conf.getEntityFacade().executeTransactionAsync(call).get(conf.getSecondsTimeout(), TimeUnit.SECONDS);

            }
            return conf.getEntityFacade().executeTransaction(() -> {
                return call.apply(conf.getEntityManager());
            });

//           
        } catch (Exception ex) {
            LOGGER.error("TX error", ex);
            throw NestedException.of(ex);
        }
    }

    public static List<LuceneFile> listAllFiles(DirConfig conf) throws IOException {
        LOGGER.trace("listAll({})", conf);
        return transactionCall(conf, em -> {
            return baseDecor(conf)
                    .buildList(em);
        });
    }

    public static IdName[] listAllIdsName(DirConfig conf) throws IOException {
        LOGGER.trace("listAllIdsName({})", conf);
        return transactionCall(conf, em -> {

            return baseDecor(conf)
                    .buildList(em).stream()
                    .map(file -> new IdName(file.getId(), file.getFileName()))
                    .toArray(s -> new IdName[s]);
        });
    }

    public static String[] listAll(DirConfig conf) throws IOException {
        LOGGER.trace("listAll({})", conf);
        return transactionCall(conf, em -> {
            return baseDecor(conf)
                    .selecting(LuceneFile_.fileName)
                    .withPred(p -> p.cb().isFalse(p.root().get(LuceneFile_.temp)))
                    .buildList(em).stream()
                    .sorted()
                    .toArray(s -> new String[s]);
        });
    }

    public static String[] listDistinctFolders(KindConfig conf) throws IOException {
        LOGGER.trace("listDistinctFolders({})", conf);
        return transactionCall(conf, em -> {
            return baseKindDecor(conf)
                    .selecting(LuceneFile_.folderName)
                    .setDistinct(true)
                    .buildList(em).stream()
                    .sorted()
                    .toArray(s -> new String[s]);
        });
    }

    public static Set<String> listTemp(DirConfig conf) throws IOException {
        LOGGER.trace("listTemp({})", conf);
        return transactionCall(conf, em -> {
            return baseDecor(conf)
                    .selecting(LuceneFile_.fileName)
                    .withPred(p -> p.cb().isTrue(p.root().get(LuceneFile_.temp)))
                    .buildList(em).stream()
                    .collect(Collectors.toSet());
        });
    }

    public static SafeOpt<Long> fileId(DirConfig conf, String fileName) throws IOException {
        LOGGER.trace("listTemp({},{})", conf, fileName);
        return transactionCall(conf, em -> {
            return baseDecor(conf, fileName)
                    .selecting(LuceneFile_.id)
                    .buildUniqueResult(em)
                    .throwIfErrorAsNested();
        });

    }

    public static boolean deleteFile(DirConfig conf, String fileName) throws IOException {
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

    public static boolean existsFile(DirConfig conf, String fileName) throws IOException {
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

    public static boolean renameFile(DirConfig conf, String src, String dst) throws IOException {
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

    public static long fileLength(DirConfig conf, String fileName) throws IOException {
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

     public static SafeOpt<Date> fileLastMofified(DirConfig conf, String fileName) throws IOException {
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
     
     public static SafeOpt<Date> fileDirectoryLastMofified(DirConfig conf) throws IOException {
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
    
    public static SafeOpt<InputStream> fileContent(DirConfig conf, String fileName) throws IOException {
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

    public static SafeOpt<byte[]> fileContentBytes(DirConfig conf, String fileName) throws IOException {
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

    public static boolean saveFile(DirConfig conf, String fileName, ExtInputStream stream, long length, boolean temp, Date date) throws IOException {
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

            return true;
        });

    }

    public static boolean saveFileBytes(DirConfig conf, String fileName, byte[] content, boolean temp, Date date) throws IOException {
        LOGGER.trace("saveFileBytes({},{})", conf, fileName);
        ByteArrayInputStream stream = new ByteArrayInputStream(content);
        ForwardingExtInputStream extInput = new ForwardingExtInputStream() {
            @Override
            public InputStream delegate() {
                return stream;
            }
        };

        return saveFile(conf, fileName, extInput, content.length, temp, date);
    }

    public static String[] listAllOld(DirConfig conf) throws IOException {

        return transactionCall(conf, em -> {
            List<Predicate> predicates = new ArrayList<>();

            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
            Root<LuceneFile> from = query.from(LuceneFile.class);
            CriteriaQuery<String> select = query.select(from.get(LuceneFile_.fileName));
            predicates.addAll(directorySelected(conf, criteriaBuilder, from));

            select = addPredicates(predicates, select);

            TypedQuery<String> createQuery = em.createQuery(select);

            return createQuery.getResultList().stream().toArray(s -> new String[s]);
        });

    }
}
