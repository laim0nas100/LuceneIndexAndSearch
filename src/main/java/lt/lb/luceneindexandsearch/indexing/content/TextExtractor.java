package lt.lb.luceneindexandsearch.indexing.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author laim0nas100
 */
public abstract class TextExtractor {

    public static class ExtractorException extends Exception {

        private static final long serialVersionUID = 7645910994150537289L;

        public ExtractorException() {
            super();
        }

        public ExtractorException(String message) {
            super(message);
        }

        public ExtractorException(Throwable cause) {
            super(cause);
        }

        public ExtractorException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static String extractAnyText(InputStream stream) throws ExtractorException {
        try {
            StringWriter stringWriter = new StringWriter();
            Metadata metadata = new Metadata();
            ContentHandler contentHandler = new BodyContentHandler(stringWriter);
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(stream, contentHandler, metadata);
            stream.close();

            return stringWriter.toString();
        } catch (IOException | TikaException | SAXException e) {
            throw new ExtractorException(e);
        }
    }
}
