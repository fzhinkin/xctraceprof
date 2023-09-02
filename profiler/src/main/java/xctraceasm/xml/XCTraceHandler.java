/*
 * JMH Profilers based on "xctrace" utility
 * Copyright (C) 2023 Filipp Zhinkin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package xctraceasm.xml;

import org.xml.sax.Attributes;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class XCTraceHandler extends XCTraceHandlerBase {
    private final TableDesc.TableType tableType;

    private final Map<Long, TraceEntry> entriesCache = new HashMap<>();

    private final Stack<TraceEntry> entries = new Stack<>();

    private final Consumer<Sample> callback;

    private Sample currentSample = null;

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


    public XCTraceHandler(TableDesc.TableType tableType, Consumer<Sample> onSample) {
        this.tableType = tableType;
        callback = onSample;
    }

    private <T extends TraceEntry> void cache(T e) {
        TraceEntry old = entriesCache.put(e.getId(), e);
        if (old != null) {
            throw new IllegalStateException("Duplicate entry for key " + e.getId() + ". New value: "
                    + e + ", old value: " + old);
        }
    }

    private <T extends TraceEntry> T get(long id) {
        TraceEntry value = entriesCache.get(id);
        if (value == null) {
            throw new IllegalStateException("Entry not found in cache for id " + id);
        }
        @SuppressWarnings("unchecked")
        T res = (T) value;
        return res;
    }

    private <T extends TraceEntry> void pushCachedOrNew(Attributes attributes, Function<Long, T> factory) {
        if (!hasRef(attributes)) {
            T value = factory.apply(parseId(attributes));
            cache(value);
            entries.push(value);
            return;
        }
        entries.push(get(parseRef(attributes)));
    }

    private <T extends TraceEntry> T pop() {
        @SuppressWarnings("unchecked")
        T res = (T) entries.pop();
        return res;
    }

    private <T extends TraceEntry> T peek() {
        @SuppressWarnings("unchecked")
        T res = (T) entries.peek();
        return res;
    }

    private LongHolder popAndUpdateLongHolder() {
        LongHolder value = pop();
        if (isNeedToParseCharacters()) {
            value.setValue(Long.parseLong(getCharacters()));
        }
        return value;
    }

    private ValueHolder<long[]> popAndUpdateEvents() {
        ValueHolder<long[]> value = pop();
        if (isNeedToParseCharacters()) {
            long[] events = Arrays.stream(getCharacters().split(" "))
                    .mapToLong(Long::parseLong).toArray();
            value.setValue(events);
        }
        return value;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // check that <schema> has required table name
        if (qName.equals("schema")) {
            String schemaName = parseName(attributes);
            if (!tableType.tableName.equals(schemaName)) {
                throw new IllegalStateException("Results contains schema with unexpected name: " + schemaName);
            }
            return;
        }
        switch (qName) {
            case TraceEntry.SAMPLE:
                currentSample = new Sample();
                break;
            case TraceEntry.SAMPLE_TIME:
            case TraceEntry.CYCLE_WEIGHT:
            case TraceEntry.WEIGHT:
            case TraceEntry.PMC_EVENT:
                // TODO: validate only one of them is observed
                pushCachedOrNew(attributes, id -> {
                    setNeedParseCharacters(true);
                    return new LongHolder(id);
                });
                break;
            case TraceEntry.BACKTRACE:
                pushCachedOrNew(attributes, id -> new ValueHolder<Frame>(id));
                break;
            case TraceEntry.BINARY:
                pushCachedOrNew(attributes, id -> new ValueHolder<>(id, parseName(attributes)));
                break;
            case TraceEntry.FRAME:
                // Addresses in cpu-* tables are always biased by 1, on both X86_64 and AArch64.
                // TODO: figure out why, because kdebug tables have correct addresses.
                pushCachedOrNew(attributes, id -> new Frame(id, parseName(attributes),
                        parseAddress(attributes) - 1L));
                break;
            case TraceEntry.PMC_EVENTS:
                pushCachedOrNew(attributes, id -> {
                    setNeedParseCharacters(true);
                    return new ValueHolder<long[]>(id);
                });
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("node")) {
            return;
        }
        switch (qName) {
            case TraceEntry.SAMPLE:
                callback.accept(currentSample);
                currentSample = null;
                break;
            case TraceEntry.SAMPLE_TIME: {
                LongHolder value = popAndUpdateLongHolder();
                currentSample.setTime(value);
                break;
            }
            case TraceEntry.CYCLE_WEIGHT:
            case TraceEntry.WEIGHT:
            case TraceEntry.PMC_EVENT:
                LongHolder value = popAndUpdateLongHolder();
                currentSample.setWeight(value);
                break;
            case TraceEntry.BACKTRACE:
                currentSample.setBacktrace(this.<ValueHolder<Frame>>pop().getValue());
                break;
            case TraceEntry.BINARY:
                ValueHolder<String> bin = pop();
                this.<Frame>peek().setBinary(bin.getValue());
                break;
            case TraceEntry.FRAME:
                Frame frame = pop();
                ValueHolder<Frame> backtrace = peek();
                // we only need a top frame
                if (backtrace.getValue() == null) {
                    backtrace.setValue(frame);
                }
                break;
            case TraceEntry.PMC_EVENTS:
                ValueHolder<long[]> events = popAndUpdateEvents();
                currentSample.setSamples(events.getValue());
                break;
        }
        setNeedParseCharacters(false);
    }

    @Override
    public void endDocument() {
        entriesCache.clear();
        entries.clear();
    }
}
