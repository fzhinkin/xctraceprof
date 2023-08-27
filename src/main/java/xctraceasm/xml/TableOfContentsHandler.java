package xctraceasm.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.util.stream.Collectors;

// TODO: validate the document
public class TableOfContentsHandler extends DefaultHandler {
    private final List<KdebugTableDesc> kdebugTables = new ArrayList<>();

    public List<KdebugTableDesc> getKdebugTables() {
        return Collections.unmodifiableList(kdebugTables);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (!qName.equals("table")) {
            return;
        }

        String schema = Objects.requireNonNull(attributes.getValue("schema"), "Schema not found");
        if (schema.equals(KdebugTableDesc.TableType.PMI_SAMPLE.schemaName)) {
            parsePmiSampleTable(attributes);
        } else if (schema.equals(KdebugTableDesc.TableType.TIME_SAMPLE.schemaName)) {
            parseTimeSampleTable(attributes);
        }
    }

    private void parsePmiSampleTable(Attributes attributes) {
        String pmiEvent = Objects.requireNonNull(attributes.getValue("pmi-event"),
                "Trigger event not found");
        if (pmiEvent.startsWith("\"") && pmiEvent.endsWith("\"")) {
            pmiEvent = pmiEvent.substring(1, pmiEvent.length() - 1);
        }
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue("pmi-threshold"),
                "Trigger threshold not found"));
        KdebugTableDesc table = new KdebugTableDesc(KdebugTableDesc.TableType.PMI_SAMPLE,
                parseEvents(attributes), pmiEvent, threshold);
        kdebugTables.add(table);
    }

    private void parseTimeSampleTable(Attributes attributes) {
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue("sample-rate-micro-seconds"),
                "Trigger threshold not found"));
        KdebugTableDesc table = new KdebugTableDesc(KdebugTableDesc.TableType.TIME_SAMPLE,
                parseEvents(attributes), "TIME_MICRO_SEC", threshold);
        kdebugTables.add(table);
    }

    private static List<String> parseEvents(Attributes attributes) {
        String events = attributes.getValue("pmc-events");
        // TODO: support names with whitespaces inside
        return Arrays.stream(events.split(" ")).map(e -> {
                    if (!e.startsWith("\"") && !e.endsWith("\"")) return e;
                    if (e.startsWith("\"") && e.endsWith("\"")) return e.substring(1, e.length() - 1);
                    throw new IllegalStateException("Can't parse pmc-events: " + events);
                }).filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
    }
}
