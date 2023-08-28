package xctraceasm.xml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.util.stream.Collectors;

// TODO: validate the document
public class TableOfContentsHandler extends DefaultHandler {
    private final List<TableDesc> kdebugTables = new ArrayList<>();

    public List<TableDesc> getKdebugTables() {
        return Collections.unmodifiableList(kdebugTables);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (!qName.equals("table")) {
            return;
        }

        String schema = Objects.requireNonNull(attributes.getValue("schema"), "Schema not found");
        if (schema.equals(TableDesc.TableType.CPU_PROFILE.tableName)) {
            kdebugTables.add(TableDesc.CPU_PROFILE);
        }
        if (schema.equals(TableDesc.TableType.COUNTERS_PROFILE.tableName)) {
            parseCountersProfile(attributes);
        }
    }

    private void parseCountersProfile(Attributes attributes) {
        String trigger = Objects.requireNonNull(attributes.getValue("trigger"));
        CountersProfileTableDesc.TriggerType triggerType = CountersProfileTableDesc.TriggerType.valueOf(trigger.toUpperCase());

        if (triggerType == CountersProfileTableDesc.TriggerType.PMI) {
            parsePmiSampleTable(attributes);
        } else if (triggerType == CountersProfileTableDesc.TriggerType.TIME) {
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
        CountersProfileTableDesc table = new CountersProfileTableDesc(CountersProfileTableDesc.TriggerType.PMI,
                parseEvents(attributes), pmiEvent, threshold);
        kdebugTables.add(table);
    }

    private void parseTimeSampleTable(Attributes attributes) {
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue("sample-rate-micro-seconds"),
                "Trigger threshold not found"));
        CountersProfileTableDesc table = new CountersProfileTableDesc(CountersProfileTableDesc.TriggerType.TIME,
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
