package lt.lb.luceneindexandsearch.config.indexing;

import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;

/**
 *
 * @author laim0nas100
 */
public interface DocumentFieldsAware {

    public DocumentFieldsConfig getDocumentFieldsConfig();

    public void setDocumentFieldsConfig(DocumentFieldsConfig documentFieldsConfig);

}
