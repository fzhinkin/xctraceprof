package xctraceasm.xml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

// TODO: validate the document
public class TableOfContentsHandler extends DefaultHandler {
    private static final DateTimeFormatter TOC_DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final List<TableDesc> supportedTables = new ArrayList<>();

    private final StringBuilder builder = new StringBuilder();

    private boolean recordChars = false;

    private long recordStartMs;

    public List<TableDesc> getSupportedTables() {
        return Collections.unmodifiableList(supportedTables);
    }

    public long getRecordStartMs() {
        return recordStartMs;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equals("start-date")) {
            recordChars = true;
        }
        if (!qName.equals("table")) {
            return;
        }

        String schema = Objects.requireNonNull(attributes.getValue("schema"), "Schema not found");
        if (schema.equals(TableDesc.TableType.CPU_PROFILE.tableName)) {
            supportedTables.add(TableDesc.CPU_PROFILE);
        }
        if (schema.equals(TableDesc.TableType.COUNTERS_PROFILE.tableName)) {
            parseCountersProfile(attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (!qName.equals("start-date")) {
            return;
        }
        recordChars = false;
        try {
            recordStartMs = Instant.from(TOC_DATE_FORMAT.parse(builder.toString())).toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (recordChars) {
            builder.append(ch, start, length);
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
        supportedTables.add(table);
    }

    private void parseTimeSampleTable(Attributes attributes) {
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue("sample-rate-micro-seconds"),
                "Trigger threshold not found"));
        CountersProfileTableDesc table = new CountersProfileTableDesc(CountersProfileTableDesc.TriggerType.TIME,
                parseEvents(attributes), "TIME_MICRO_SEC", threshold);
        supportedTables.add(table);
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
