package lt.lb.luceneindexandsearch.indexing;

import java.util.Objects;
import java.util.function.BiFunction;
import lt.lb.luceneindexandsearch.config.IndexedFieldConfig;
import org.apache.lucene.document.Field;

/**
 *
 * @author laim0nas100
 */
public class SimpleIndexedFieldConfig implements IndexedFieldConfig {

    protected boolean validated = false;
    protected String name;
    protected boolean storedField;
    protected boolean contentField;
    protected boolean mainIdField;
    protected boolean idField;
    protected boolean optional;
    protected BiFunction<IndexedFieldConfig, String, ? extends Field> fieldMaker;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Field makeField(String content) {
        return fieldMaker.apply(this, content);
    }

    @Override
    public boolean isStoredField() {
        return storedField;
    }

    @Override
    public boolean isContentField() {
        return contentField;
    }

    @Override
    public boolean isIdField() {
        return idField;
    }

    public void setIdField(boolean idField) {
        this.idField = idField;
    }

    @Override
    public boolean isMainIdField() {
        return mainIdField;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    public void validate() {
        if (validated) {
            return;
        }
        Objects.requireNonNull(fieldMaker, "Field maker not configured");
        if(mainIdField && idField){
            throw new IllegalArgumentException("Can't be id and mainId at the same time");
        }
        if (mainIdField || idField) {
            if (optional) {
                throw new IllegalArgumentException("Can't be id or mainId and optional");
            }
            if (!storedField) {
                throw new IllegalArgumentException("id or mainId field must be stored");
            }
            if (contentField) {
                throw new IllegalArgumentException("Field cannot be content, and of of ids");
            }
        } 
        if (!mainIdField && !idField && !contentField && !storedField ) {
            throw new IllegalArgumentException("Field must be satisfy at least one property of [mainId, id, content, stored], otherwise field is not usefull.");
        }

        validated = true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStoredField(boolean storedField) {
        this.storedField = storedField;
    }

    public void setContentField(boolean contentField) {
        this.contentField = contentField;
    }

    public void setMainIdField(boolean mainIdField) {
        this.mainIdField = mainIdField;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public void setFieldMaker(BiFunction<IndexedFieldConfig, String, ? extends Field> fieldMaker) {
        this.fieldMaker = fieldMaker;
    }

}
