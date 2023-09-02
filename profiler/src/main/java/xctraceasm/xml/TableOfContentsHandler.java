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
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

// TODO: validate the document
public class TableOfContentsHandler extends XCTraceHandlerBase {
    private static final DateTimeFormatter TOC_DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final List<TableDesc> supportedTables = new ArrayList<>();

    private long recordStartMs;

    public List<TableDesc> getSupportedTables() {
        return Collections.unmodifiableList(supportedTables);
    }

    public long getRecordStartMs() {
        return recordStartMs;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        setNeedParseCharacters(qName.equals("start-date"));
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
        try {
            recordStartMs = Instant.from(TOC_DATE_FORMAT.parse(getCharacters())).toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(e);
        } finally {
            setNeedParseCharacters(false);
        }
    }

    private void parseCountersProfile(Attributes attributes) {
        String trigger = Objects.requireNonNull(attributes.getValue("trigger"));
        TableDesc.TriggerType triggerType = TableDesc.TriggerType.valueOf(trigger.toUpperCase());

        if (triggerType == TableDesc.TriggerType.PMI) {
            parsePmiSampleTable(attributes);
        } else if (triggerType == TableDesc.TriggerType.TIME) {
            parseTimeSampleTable(attributes);
        } else {
            throw new IllegalStateException("Unsupported trigger type: " + triggerType);
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
        TableDesc table = new TableDesc(TableDesc.TableType.COUNTERS_PROFILE, TableDesc.TriggerType.PMI,
                parseEvents(attributes), pmiEvent, threshold);
        supportedTables.add(table);
    }

    private void parseTimeSampleTable(Attributes attributes) {
        long threshold = Long.parseLong(Objects.requireNonNull(attributes.getValue("sample-rate-micro-seconds"),
                "Trigger threshold not found"));
        TableDesc table = new TableDesc(TableDesc.TableType.COUNTERS_PROFILE, TableDesc.TriggerType.TIME,
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
