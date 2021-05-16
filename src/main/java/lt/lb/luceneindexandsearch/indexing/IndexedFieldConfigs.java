package lt.lb.luceneindexandsearch.indexing;

import lt.lb.luceneindexandsearch.config.IndexedFieldConfig;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

/**
 *
 * @author laim0nas100
 */
public class IndexedFieldConfigs {

    public static Field makeField(IndexedFieldConfig config, String content) {
        String fieldName = config.getName();
        boolean stored = config.isStoredField();
        Field.Store store = stored ? Field.Store.YES : Field.Store.NO;
        if ((config.isMainIdField() || config.isIdField()) && config.isContentField()) {
            throw new IllegalArgumentException("Field can be content or id, not both");
        }
        if (config.isMainIdField() || config.isIdField()) {
            if (!stored) {
                throw new IllegalArgumentException("id or mainId field must be stored");
            }
            return new StringField(fieldName, content, store);
        }
        if (config.isContentField()) {
            return new TextField(fieldName, content, store);
        }
        if (store == Field.Store.YES) {
            return new StoredField(fieldName, content);
        }
        throw new IllegalArgumentException("Field is not id, content nor stored - field is not usefull. Reconfigure it.");
    }

    private static SimpleIndexedFieldConfig conf(String name) {
        SimpleIndexedFieldConfig config = new SimpleIndexedFieldConfig();
        config.setName(name);
        config.setContentField(false);
        config.setOptional(false);
        config.setStoredField(false);
        config.setMainIdField(false);
        config.setIdField(false);
        config.setFieldMaker(IndexedFieldConfigs::makeField);
        return config;
    }

    public static IndexedFieldConfig makeMainId(String name) {
        SimpleIndexedFieldConfig config = conf(name);
        config.setStoredField(true);
        config.setMainIdField(true);
        config.validate();
        return config;
    }

    public static IndexedFieldConfig makeId(String name) {
        SimpleIndexedFieldConfig config = conf(name);
        config.setStoredField(true);
        config.setIdField(true);
        config.validate();
        return config;
    }

    public static IndexedFieldConfig makeStored(String name) {
        SimpleIndexedFieldConfig config = conf(name);
        config.setStoredField(true);
        config.validate();
        return config;
    }

    public static IndexedFieldConfig makeStoredOptional(String name) {
        SimpleIndexedFieldConfig config = conf(name);
        config.setOptional(true);
        config.setStoredField(true);
        config.validate();
        return config;
    }

    public static IndexedFieldConfig makeContentOptional(String name) {
        SimpleIndexedFieldConfig config = conf(name);
        config.setContentField(true);
        config.setOptional(true);
        config.validate();
        return config;
    }

    public static IndexedFieldConfig makeContent(String name) {
        SimpleIndexedFieldConfig config = conf(name);
        config.setContentField(true);
        config.validate();
        return config;
    }

}
