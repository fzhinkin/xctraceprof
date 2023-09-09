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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

// TODO: validate the document
class XCTraceTableOfContentsHandler extends XCTraceHandlerBase {
    private static final DateTimeFormatter TOC_DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final List<XCTraceTableDesc> supportedTables = new ArrayList<>();

    private long recordStartMs;

    private static List<String> parseEvents(Attributes attributes) {
        String events = attributes.getValue(XCTraceHandlerBase.PMC_EVENTS);
        // TODO: support names with whitespaces inside
        return Arrays.stream(events.split(" ")).map(e -> {
                    if (!e.startsWith("\"") && !e.endsWith("\"")) return e;
                    if (e.startsWith("\"") && e.endsWith("\"")) return e.substring(1, e.length() - 1);
                    throw new IllegalStateException("Can't parse pmc-events: " + events);
                }).filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
    }

    public List<XCTraceTableDesc> getSupportedTables() {
        return Collections.unmodifiableList(supportedTables);
    }

    public long getRecordStartMs() {
        return recordStartMs;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        setNeedParseCharacters(qName.equals(XCTraceHandlerBase.START_DATE));
        if (!qName.equals(XCTraceHandlerBase.TABLE)) {
            return;
        }

        String schema = Objects.requireNonNull(attributes.getValue(XCTraceHandlerBase.SCHEMA), "Schema not found");
        if (schema.equals(XCTraceTableDesc.TableType.CPU_PROFILE.tableName)) {
            supportedTables.add(XCTraceTableDesc.CPU_PROFILE);
        } else if (schema.equals(XCTraceTableDesc.TableType.TIME_PROFILE.tableName)) {
            supportedTables.add(XCTraceTableDesc.TIME_PROFILE);
        } else if (schema.equals(XCTraceTableDesc.TableType.COUNTERS_PROFILE.tableName)) {
            parseCountersProfile(attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (!qName.equals(XCTraceHandlerBase.START_DATE)) {
            return;
        }
        try {
            recordStartMs = Instant.from(TOC_DATE_FORMAT.parse(getCharacters())).toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(e);
        } finally {
            setNeedParseCharacters(false);
        }
    }

    private void parseCountersProfile(Attributes attributes) {
        String trigger = Objects.requireNonNull(attributes.getValue(XCTraceHandlerBase.TRIGGER));
        XCTraceTableDesc.TriggerType triggerType = XCTraceTableDesc.TriggerType.valueOf(trigger.toUpperCase());

        if (triggerType == XCTraceTableDesc.TriggerType.PMI) {
            parsePmiSampleTable(attributes);
        } else if (triggerType == XCTraceTableDesc.TriggerType.TIME) {
            parseTimeSampleTable(attributes);
        } else {
            throw new IllegalStateException("Unsupported trigger type: " + triggerType);
        }
    }

    private void parsePmiSampleTable(Attributes attributes) {
        String pmiEvent = Objects.requireNonNull(attributes.getValue(XCTraceHandlerBase.PMI_EVENT),
                "Trigger event not found");
        if (pmiEvent.startsWith("\"") && pmiEvent.endsWith("\"")) {
            pmiEvent = pmiEvent.substring(1, pmiEvent.length() - 1);
        }
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue(XCTraceHandlerBase.PMI_THRESHOLD),
                "Trigger threshold not found"));
        XCTraceTableDesc table = new XCTraceTableDesc(XCTraceTableDesc.TableType.COUNTERS_PROFILE, XCTraceTableDesc.TriggerType.PMI,
                parseEvents(attributes), pmiEvent, threshold);
        supportedTables.add(table);
    }

    private void parseTimeSampleTable(Attributes attributes) {
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue(XCTraceHandlerBase.SAMPLE_RATE),
                "Trigger threshold not found"));
        XCTraceTableDesc table = new XCTraceTableDesc(XCTraceTableDesc.TableType.COUNTERS_PROFILE, XCTraceTableDesc.TriggerType.TIME,
                parseEvents(attributes), XCTraceSample.TIME_SAMPLE_TRIGGER_NAME, threshold);
        supportedTables.add(table);
    }
}
