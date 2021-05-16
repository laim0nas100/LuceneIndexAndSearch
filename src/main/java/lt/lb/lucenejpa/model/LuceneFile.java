package lt.lb.lucenejpa.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.validation.constraints.NotNull;

/**
 *
 * Single Lucene file entity. Multiple directories in a single table.
 *
 * @author laim0nas100
 */
@Entity
@Table(name = "LUCENE_FILE")
public class LuceneFile implements Serializable {

    private Long id;
    private String fileName;
    private String folderName;
    private String fileKind;
    private String fileOrigin;

    private long fileSize;
    private Date lastModified;
    private boolean temp;

    private LuceneBlob luceneBlob;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    public Long getId() {
        return id;
    }

    @NotNull
    @Column(name = "FILE_NAME")
    public String getFileName() {
        return fileName;
    }

    @NotNull
    @Column(name = "FOLDER_NAME")
    public String getFolderName() {
        return folderName;
    }

    @NotNull
    @Column(name = "FILE_KIND")
    public String getFileKind() {
        return fileKind;
    }

    @NotNull
    @Column(name = "FILE_ORIGIN")
    public String getFileOrigin() {
        return fileOrigin;
    }

    @NotNull
    @Column(name = "FILE_SIZE")
    public long getFileSize() {
        return fileSize;
    }

    @Column(name = "LAST_MODIFIED")
    @NotNull
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getLastModified() {
        return lastModified;
    }

    @Column(name = "TEMP")
    public boolean isTemp() {
        return temp;
    }

    @JoinColumn(name = "LUCENE_BLOB_ID")
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public LuceneBlob getLuceneBlob() {
        return luceneBlob;
    }

    public void setLuceneBlob(LuceneBlob luceneBlob) {
        this.luceneBlob = luceneBlob;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setFileKind(String fileKind) {
        this.fileKind = fileKind;
    }

    public void setFileOrigin(String fileOrigin) {
        this.fileOrigin = fileOrigin;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setTemp(boolean temp) {
        this.temp = temp;
    }

    @Override
    public String toString() {
        return "LuceneFile{" + "id=" + id + ", fileName=" + fileName + ", folderName=" + folderName + ", fileKind=" + fileKind + ", fileOrigin=" + fileOrigin + ", fileSize=" + fileSize + ", lastModified=" + lastModified + ", temp=" + temp + '}';
    }

}
