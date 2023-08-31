package xctraceasm.xml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.util.function.Consumer;

public class XCTraceHandler extends DefaultHandler {
    private final TableDesc.TableType tableType;

    private final Map<Long, TraceEntry> entriesCache = new HashMap<>();

    private final Stack<TraceEntry> entries = new Stack<>();

    private final Consumer<Sample> callback;

    private final StringBuilder builder = new StringBuilder();

    private Sample currentSample = null;

    private boolean needParse = false;

    private boolean skipNodes = false;

    private boolean withinNode = false;

    private boolean observedRequiredTable = false;


    private static long parseId(Attributes attributes) {
        return Long.parseLong(attributes.getValue("id"));
    }

    private static long parseRef(Attributes attributes) {
        return Long.parseLong(attributes.getValue("ref"));
    }

    private static long parseAddress(Attributes attributes) {
        String val = attributes.getValue("addr");
        if (!val.startsWith("0x")) throw new IllegalStateException("Unexpected addr format: " + val);
        try {
            return Long.parseUnsignedLong(val.substring(2), 16);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + val, e);
        }
    }

    private static String parseName(Attributes attributes) {
        return attributes.getValue("name");
    }

    private static boolean hasRef(Attributes attributes) {
        return attributes.getValue("ref") != null;
    }

    private TraceEntry getCached(long ref) {
        TraceEntry entry = entriesCache.get(ref);
        Objects.requireNonNull(entry, "Entry not found in cache: ref=\"" + ref + "\"");
        return entry;
    }

    private <T extends TraceEntry> T cache(T entry) {
        entriesCache.put(entry.getId(), entry);
        return entry;
    }

    public XCTraceHandler(TableDesc.TableType tableType, Consumer<Sample> onSample) {
        this.tableType = tableType;
        callback = onSample;
    }

    public boolean observedCpuProfileSchema() {
        return observedRequiredTable;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equals("node")) {
            withinNode = true;
            return;
        }
        if (withinNode && qName.equals("schema")) {
            String schemaName = parseName(attributes);
            skipNodes = !tableType.tableName.equals(schemaName);
            observedRequiredTable = observedRequiredTable || !skipNodes;
            return;
        }
        if (skipNodes) return;
        builder.setLength(0);
        switch (qName) {
            case TraceEntry.SAMPLE:
                currentSample = new Sample();
                break;
            case TraceEntry.SAMPLE_TIME:
                if (hasRef(attributes)) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    entries.push(cache(new LongHolder(parseId(attributes))));
                    needParse = true;
                }
                break;
            case TraceEntry.CYCLE_WEIGHT:
            case TraceEntry.WEIGHT:
            case TraceEntry.PMC_EVENT:
                // TODO: validate only one of them is observed
                if (hasRef(attributes)) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    entries.push(cache(new LongHolder(parseId(attributes))));
                    needParse = true;
                }
                break;
            case TraceEntry.BACKTRACE:
                if (hasRef(attributes)) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    entries.push(cache(new Backtrace(parseId(attributes))));
                }
                break;
            case TraceEntry.BINARY:
                if (hasRef(attributes)) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    Binary bin = new Binary(parseId(attributes));
                    bin.setName(parseName(attributes));
                    entries.push(cache(bin));
                }
                break;
            case TraceEntry.FRAME:
                if (hasRef(attributes)) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    Frame frame = new Frame(parseId(attributes));
                    frame.setName(parseName(attributes));
                    // Addresses in cpu-* tables are always biased by 1, on both X86_64 and AArch64.
                    // TODO: figure out why, because kdebug tables have correct addresses.
                    frame.setAddress(parseAddress(attributes) - 1L);
                    entries.push(cache(frame));
                }
                break;
            case TraceEntry.PMC_EVENTS:
                if (hasRef(attributes)) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    PmcEvents events = new PmcEvents(parseId(attributes));
                    entries.push(cache(events));
                    needParse = true;
                }
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (!skipNodes && needParse) {
            builder.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("node")) {
            skipNodes = false;
            return;
        }
        if (skipNodes) return;
        switch (qName) {
            case TraceEntry.SAMPLE:
                callback.accept(currentSample);
                currentSample = null;
                break;
            case TraceEntry.SAMPLE_TIME:
                LongHolder time = (LongHolder) entries.pop();
                if (needParse) {
                    time.setValue(Long.parseLong(builder.toString()));
                }
                currentSample.setTime(time);
                needParse = false;
                break;
            case TraceEntry.CYCLE_WEIGHT:
            case TraceEntry.WEIGHT:
            case TraceEntry.PMC_EVENT:
                LongHolder weight = (LongHolder) entries.pop();
                if (needParse) {
                    weight.setValue(Long.parseLong(builder.toString()));
                }
                currentSample.setWeight(weight);
                needParse = false;
                break;
            case TraceEntry.BACKTRACE:
                Backtrace bt = (Backtrace) entries.pop();
                currentSample.setBacktrace(bt);
                break;
            case TraceEntry.BINARY:
                Binary bin = (Binary) entries.pop();
                ((Frame) entries.peek()).setBinary(bin);
                break;
            case TraceEntry.FRAME:
                Frame frame = (Frame) entries.pop();
                Backtrace backtrace = ((Backtrace) entries.peek());
                // we only need a top frame
                if (backtrace.isEmpty()) {
                    backtrace.addFrame(frame);
                }
                break;
            case TraceEntry.PMC_EVENTS:
                PmcEvents events = (PmcEvents) entries.pop();
                if (needParse) {
                    events.setCounters(Arrays.stream(builder.toString().split(" "))
                            .mapToLong(Long::parseLong).toArray());
                }
                currentSample.setSamples(events.getCounters());
                needParse = false;
                break;
        }
    }

    @Override
    public void endDocument() {
        entriesCache.clear();
        entries.clear();
    }
}
