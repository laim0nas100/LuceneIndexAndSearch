package lt.lb.lucenejpa.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 *
 * @author laim0nas100
 */
@Entity
@Table(name = "LUCENE_BLOB")
public class LuceneBlob {

    private Long id;
    private byte[] content;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    public Long getId() {
        return id;
    }

    @Lob
    @Column(name = "CONTENT", columnDefinition = "BLOB", nullable = true)
    public byte[] getContent() {
        return content;
    }

//     @Lob
//    @Column(name = "CONTENT", columnDefinition = "BLOB")
//    public Blob getContent() {
//        return content;
//    }
    public void setContent(byte[] content) {
        this.content = content;
    }

//    public void setContent(Blob content) {
//        this.content = content;
//    }
    public void setId(Long id) {
        this.id = id;
    }

}
