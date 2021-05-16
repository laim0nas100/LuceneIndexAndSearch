package lt.lb.luceneindexandsearch.indexing;

import lt.lb.commons.parsing.StringParser;
import lt.lb.uncheckedutils.SafeOpt;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author laim0nas100
 */
public class DocFieldFunctor implements StringParser<Document> {

    public final String fieldName;

    public DocFieldFunctor(String fieldName) {
        this.fieldName = fieldName;
    }

    public SafeOpt<IndexableField> getFieldOpt(Document doc) {
        return SafeOpt.ofGet(() -> doc.getField(fieldName));
    }

    @Override
    public SafeOpt<String> parseOptString(Document doc) {
        return getFieldOpt(doc).map(m -> m.stringValue());
    }

}
