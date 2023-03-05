package lt.lb.luceneindexandsearch.splitting;

import lt.lb.commons.jpa.EntityFacade;

/**
 *
 * @author laim0nas100
 */
public class JpaDirConfigSimple implements JpaDirConfig {

    protected KindConfig kind;
    protected String folderName;
    protected EntityFacade ef;

    public JpaDirConfigSimple(KindConfig kind, String folderName, EntityFacade ef) {
        this.kind = kind;
        this.folderName = folderName;
        this.ef = ef;
    }

    @Override
    public EntityFacade getEntityFacade() {
        return ef;
    }

    @Override
    public KindConfig getKindConfig() {
        return kind;
    }

    @Override
    public String getFolderName() {
        return folderName;
    }

}
