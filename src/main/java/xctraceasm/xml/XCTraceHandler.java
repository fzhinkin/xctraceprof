package xctraceasm.xml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;

public class XCTraceHandler extends DefaultHandler {
    private static final String SCHEMA = "cpu-profile";

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
        if (!val.startsWith("0x")) throw new IllegalStateException("Unexpected addr formats: " + val);
        try {
            return Long.parseUnsignedLong(val.substring(2), 16);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + val, e);
        }
    }

    private static String parseName(Attributes attributes) {
        return attributes.getValue("name");
    }

    private static boolean has(Attributes attributes, String id) {
        return attributes.getValue(id) != null;
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

    public XCTraceHandler(Consumer<Sample> onSample) {
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
            skipNodes = !parseName(attributes).equals(SCHEMA);
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
                if (has(attributes, "ref")) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    entries.push(cache(new SampleTime(parseId(attributes))));
                    needParse = true;
                }
                break;
            case TraceEntry.CYCLE_WEIGHT:
                if (has(attributes, "ref")) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    entries.push(cache(new CycleWeight(parseId(attributes))));
                    needParse = true;
                }
                break;
            case TraceEntry.BACKTRACE:
                if (has(attributes, "ref")) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    entries.push(cache(new Backtrace(parseId(attributes))));
                }
                break;
            case TraceEntry.BINARY:
                if (has(attributes, "ref")) {
                    entries.push(getCached(parseRef(attributes)));
                } else {
                    Binary bin = new Binary(parseId(attributes));
                    bin.setName(parseName(attributes));
                    entries.push(cache(bin));
                }
                break;
            case TraceEntry.FRAME:
                if (has(attributes, "ref")) {
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
                SampleTime time = (SampleTime) entries.pop();
                if (needParse) {
                    time.setTimeFromStartNs(Long.parseLong(builder.toString()));
                }
                currentSample.setTime(time);
                needParse = false;
                break;
            case TraceEntry.CYCLE_WEIGHT:
                CycleWeight weight = (CycleWeight) entries.pop();
                if (needParse) {
                    weight.setWeight(Long.parseLong(builder.toString()));
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
                ((Backtrace) entries.peek()).addFrame(frame);
                break;
        }
    }

    @Override
    public void endDocument() {
        entriesCache.clear();
        entries.clear();
    }
}
