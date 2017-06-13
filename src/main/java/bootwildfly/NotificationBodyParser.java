package bootwildfly;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationBodyParser {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationBodyParser.class);

    public static Map<String, String> retrieveData(String xml, Collection<String> fields) {

        Map<String, String> result = new HashMap<>();

        try {
            XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(xml.getBytes()));
            while (xmlReader.hasNext()) {
                xmlReader.next();
                if (xmlReader.isStartElement()) {
                    if (fields.contains(xmlReader.getLocalName()) && xmlReader.hasNext()) {

                        String localName = xmlReader.getLocalName();
                        xmlReader.next();

                        if (xmlReader.isCharacters()) {
                            result.put(localName, xmlReader.getText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Exception occured while parsing notification body. ", e);
        }
        return result;
    }
}
