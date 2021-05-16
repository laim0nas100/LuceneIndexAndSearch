package lt.lb.luceneindexandsearch.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 *
 * @author laim0nas100
 */
public interface DocumentFieldsConfig {

    public default String[] getContentFieldsArray() {
        return getContentFieldNames().stream().toArray(s -> new String[s]);
    }

    public String getMainIdFieldName();

    public Collection<String> getContentFieldNames();

    public Collection<IndexedFieldConfig> getFields();

    public Optional<IndexedFieldConfig> getField(String fieldName);

    public default Collection<String> getMandatoryFieldNames() {
        return getFields().stream()
                .filter(field -> !field.isOptional()).map(m -> m.getName())
                .collect(Collectors.toList());
    }
    
    public default Collection<String> getFieldNames() {
        return getFields().stream()
                .map(m -> m.getName())
                .collect(Collectors.toList());
    }

    public default Document createDocument(Map<String, String> fieldContent) {

        ArrayList<Field> fields = new ArrayList<>();

        HashSet<String> unusedFields = new HashSet<>(getMandatoryFieldNames());
        for (Map.Entry<String, String> fc : fieldContent.entrySet()) {
            String name = fc.getKey();
            String content = fc.getValue();
            getField(name).map(m -> m.makeField(content));
            Optional<IndexedFieldConfig> fieldConfig = getField(name);
            if (!fieldConfig.isPresent()) {
                throw new IllegalArgumentException("No such field defined:" + name);
            }
            Field field = fieldConfig.get().makeField(content);
            fields.add(field);
            unusedFields.remove(name);
        }

        if (!unusedFields.isEmpty()) {
            throw new IllegalArgumentException("Not all mandatory fields has been supplied:"+unusedFields);
        }
        Document doc = new Document();
        for (Field field : fields) {
            doc.add(field);
        }
        return doc;

    }

}
