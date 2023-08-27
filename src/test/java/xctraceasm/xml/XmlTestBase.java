package xctraceasm.xml;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

public abstract class XmlTestBase {
    protected final SAXParserFactory factory = SAXParserFactory.newInstance();

    protected static InputStream openResource(String name) {
        InputStream stream = XmlTestBase.class.getResourceAsStream("/" + name);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + name);
        }
        return stream;
    }
}
