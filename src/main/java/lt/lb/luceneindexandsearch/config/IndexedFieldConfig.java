package lt.lb.luceneindexandsearch.config;

import org.apache.lucene.document.Field;

/**
 *
 * @author laim0nas100
 */
public interface IndexedFieldConfig {

    public String getName();

    public Field makeField(String content);

    public boolean isStoredField();

    public boolean isContentField();

    public boolean isMainIdField();
    
    public boolean isIdField();

    public boolean isOptional();
    
}
