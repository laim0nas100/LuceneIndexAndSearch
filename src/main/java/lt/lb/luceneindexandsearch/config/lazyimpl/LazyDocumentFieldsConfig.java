package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.containers.caching.lazy.LazyProxy;
import lt.lb.commons.containers.caching.lazy.LazyValueThreaded;
import lt.lb.luceneindexandsearch.config.DocumentFieldsConfig;
import lt.lb.luceneindexandsearch.config.IndexedFieldConfig;

/**
 *
 * @author laim0nas100
 */
public class LazyDocumentFieldsConfig implements DocumentFieldsConfig {

    protected HashMap<String, IndexedFieldConfig> fieldMap = new HashMap<>();

    protected LazyProxy<Map<String, IndexedFieldConfig>> lazyMap = new LazyValueThreaded<>(fieldMap);
    protected LazyProxy<String[]> contentFieldsArray = lazyMap.map(map -> {
        return map.values().stream()
                .filter(f -> f.isContentField())
                .map(m -> m.getName())
                .toArray(s -> new String[s]);
    });
    protected LazyProxy<Collection<String>> mandatoryFields = lazyMap.map(map -> {
        return map.values().stream()
                .filter(f -> !f.isOptional())
                .map(m -> m.getName())
                .collect(Collectors.toList());
    });
    protected LazyProxy<IndexedFieldConfig> mainIdField = lazyMap.map(map -> {
        List<IndexedFieldConfig> collect = map.values().stream()
                .filter(f -> (f.isMainIdField() && !f.isOptional()))
                .collect(Collectors.toList());
        if (collect.size() != 1) {
            throw new IllegalArgumentException("Main id field must be defined unique and not optional!");
        }
        return collect.get(0);
    });

    public void addFields(IndexedFieldConfig... configs) {
        for (IndexedFieldConfig conf : configs) {
            fieldMap.put(conf.getName(), conf);

        }
        lazyMap.invalidate();
    }

    public void clearFields() {
        fieldMap.clear();
        lazyMap.invalidate();
    }

    @Override
    public String[] getContentFieldsArray() {
        return contentFieldsArray.get();
    }

    @Override
    public String getMainIdFieldName() {
        return mainIdField.get().getName();
    }

    @Override
    public Collection<String> getContentFieldNames() {
        return Stream.of(getContentFieldsArray()).collect(Collectors.toList());
    }

    @Override
    public Collection<IndexedFieldConfig> getFields() {
        return new ArrayList<>(lazyMap.get().values());
    }

    @Override
    public Optional<IndexedFieldConfig> getField(String fieldName) {
        return Optional.ofNullable(lazyMap.get().get(fieldName));
    }

    @Override
    public Collection<String> getMandatoryFieldNames() {
        return mandatoryFields.get();
    }


}
