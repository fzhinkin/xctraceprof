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

package org.openjdk.jmh.profile;

import org.xml.sax.Attributes;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

class XCTraceTableHandler extends XCTraceHandlerBase {
    private final XCTraceTableDesc.TableType tableType;

    private final Map<Long, TraceElement> entriesCache = new HashMap<>();

    private final Stack<TraceElement> entries = new Stack<>();

    private final Consumer<XCTraceSample> callback;

    private XCTraceSample currentSample = null;

    private static long parseId(Attributes attributes) {
        return Long.parseLong(attributes.getValue(XCTraceHandlerBase.ID));
    }

    private static long parseRef(Attributes attributes) {
        return Long.parseLong(attributes.getValue(XCTraceHandlerBase.REF));
    }

    private static long parseAddress(Attributes attributes) {
        String val = attributes.getValue(XCTraceHandlerBase.ADDRESS);
        if (!val.startsWith("0x")) throw new IllegalStateException("Unexpected address format: " + val);
        try {
            return Long.parseUnsignedLong(val.substring(2), 16);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse " + val, e);
        }
    }

    private static String parseName(Attributes attributes) {
        return attributes.getValue(XCTraceHandlerBase.NAME);
    }

    private static boolean hasRef(Attributes attributes) {
        return attributes.getValue(XCTraceHandlerBase.REF) != null;
    }


    public XCTraceTableHandler(XCTraceTableDesc.TableType tableType, Consumer<XCTraceSample> onSample) {
        this.tableType = tableType;
        callback = onSample;
    }

    private <T extends TraceElement> void cache(T e) {
        TraceElement old = entriesCache.put(e.id, e);
        if (old != null) {
            throw new IllegalStateException("Duplicate entry for key " + e.id + ". New value: "
                    + e + ", old value: " + old);
        }
    }

    private <T extends TraceElement> T get(long id) {
        TraceElement value = entriesCache.get(id);
        if (value == null) {
            throw new IllegalStateException("Entry not found in cache for id " + id);
        }
        @SuppressWarnings("unchecked")
        T res = (T) value;
        return res;
    }

    private <T extends TraceElement> void pushCachedOrNew(Attributes attributes, Function<Long, T> factory) {
        if (!hasRef(attributes)) {
            T value = factory.apply(parseId(attributes));
            cache(value);
            entries.push(value);
            return;
        }
        entries.push(get(parseRef(attributes)));
    }

    private <T extends TraceElement> T pop() {
        @SuppressWarnings("unchecked")
        T res = (T) entries.pop();
        return res;
    }

    private <T extends TraceElement> T peek() {
        @SuppressWarnings("unchecked")
        T res = (T) entries.peek();
        return res;
    }

    private LongHolder popAndUpdateLongHolder() {
        LongHolder value = pop();
        if (isNeedToParseCharacters()) {
            value.value = Long.parseLong(getCharacters());
        }
        return value;
    }

    private ValueHolder<long[]> popAndUpdateEvents() {
        ValueHolder<long[]> value = pop();
        if (isNeedToParseCharacters()) {
            value.value = Arrays.stream(getCharacters().split(" "))
                    .mapToLong(Long::parseLong).toArray();
        }
        return value;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // check that <schema> has required table name
        if (qName.equals(XCTraceHandlerBase.SCHEMA)) {
            String schemaName = parseName(attributes);
            if (!tableType.tableName.equals(schemaName)) {
                throw new IllegalStateException("Results contains schema with unexpected name: " + schemaName);
            }
            return;
        }
        switch (qName) {
            case XCTraceHandlerBase.SAMPLE:
                currentSample = new XCTraceSample();
                break;
            case XCTraceHandlerBase.SAMPLE_TIME:
            case XCTraceHandlerBase.CYCLE_WEIGHT:
            case XCTraceHandlerBase.WEIGHT:
            case XCTraceHandlerBase.PMC_EVENT:
                // TODO: validate only one of them is observed
                pushCachedOrNew(attributes, id -> {
                    setNeedParseCharacters(true);
                    return new LongHolder(id);
                });
                break;
            case XCTraceHandlerBase.BACKTRACE:
                pushCachedOrNew(attributes, id -> new ValueHolder<Frame>(id));
                break;
            case XCTraceHandlerBase.BINARY:
                pushCachedOrNew(attributes, id -> new ValueHolder<>(id, parseName(attributes)));
                break;
            case XCTraceHandlerBase.FRAME:
                // Addresses in cpu-* tables are always biased by 1, on both X86_64 and AArch64.
                // TODO: figure out why, because kdebug tables have correct addresses.
                pushCachedOrNew(attributes, id -> new Frame(id, parseName(attributes),
                        parseAddress(attributes) - 1L));
                break;
            case XCTraceHandlerBase.PMC_EVENTS:
                pushCachedOrNew(attributes, id -> {
                    setNeedParseCharacters(true);
                    return new ValueHolder<long[]>(id);
                });
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals(XCTraceHandlerBase.NODE)) {
            return;
        }
        switch (qName) {
            case XCTraceHandlerBase.SAMPLE:
                callback.accept(currentSample);
                currentSample = null;
                break;
            case XCTraceHandlerBase.SAMPLE_TIME: {
                LongHolder value = popAndUpdateLongHolder();
                currentSample.setTime(value.value);
                break;
            }
            case XCTraceHandlerBase.CYCLE_WEIGHT:
            case XCTraceHandlerBase.WEIGHT:
            case XCTraceHandlerBase.PMC_EVENT:
                LongHolder value = popAndUpdateLongHolder();
                currentSample.setWeight(value.value);
                break;
            case XCTraceHandlerBase.BACKTRACE:
                Frame topFrame = this.<ValueHolder<Frame>>pop().value;
                currentSample.setTopFrame(topFrame.address, topFrame.name, topFrame.binary);
                break;
            case XCTraceHandlerBase.BINARY:
                ValueHolder<String> bin = pop();
                this.<Frame>peek().binary = bin.value;
                break;
            case XCTraceHandlerBase.FRAME:
                Frame frame = pop();
                ValueHolder<Frame> backtrace = peek();
                // we only need a top frame
                if (backtrace.value == null) {
                    backtrace.value = frame;
                }
                break;
            case XCTraceHandlerBase.PMC_EVENTS:
                ValueHolder<long[]> events = popAndUpdateEvents();
                currentSample.setSamples(events.value);
                break;
        }
        setNeedParseCharacters(false);
    }

    @Override
    public void endDocument() {
        entriesCache.clear();
        entries.clear();
    }

    private static abstract class TraceElement {

        public final long id;

        public TraceElement(long id) {
            this.id = id;
        }

    }

    private static final class ValueHolder<T> extends TraceElement {
        public T value;

        ValueHolder(long id, T value) {
            super(id);
            this.value = value;
        }

        ValueHolder(long id) {
            this(id, null);
        }

    }

    private static final class LongHolder extends TraceElement {
        public long value = 0;

        public LongHolder(long id) {
            super(id);
        }

    }

    private static final class Frame extends TraceElement {
        public final String name;

        public final long address;

        public String binary = null;

        public Frame(long id, String name, long address) {
            super(id);
            this.name = name;
            this.address = address;
        }
    }
}
