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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.util.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class XCTraceAsmProfiler extends AbstractPerfAsmProfiler {
    private OptionSpec<String> templateOpt;

    private OptionSpec<String> tableOpt;

    private final String template;
    private final XCTraceTableDesc.TableType tableType;

    private XCTraceTableDesc.TableType foundTable;

    private static class AddressInterval {
        private long min;
        private long max;

        public AddressInterval(long address) {
            min = address;
            max = address;
        }

        void add(long address) {
            min = Math.min(min, address);
            max = Math.max(max, address);
        }

        public long getMin() {
            return min;
        }

        public long getMax() {
            return max;
        }
    }

    /**
     * Parameterless constructor to allow loading the class via ServiceLoader
     * on all platforms. To initialize the profiler, JMH will use constructor accepting
     * a string first, that's where the ProfilerException will be thrown if the profiler
     * is not available on a target machine.
     * Without it, -lprof will fail on machines where xctrace is unavailable.
     */
    public XCTraceAsmProfiler() throws ProfilerException {
        super("");
        template = "";
        tableType = XCTraceTableDesc.TableType.COUNTERS_PROFILE;
    }

    public XCTraceAsmProfiler(String initLine) throws ProfilerException {
        super(initLine, "sampled_pc");
        XCTraceUtils.checkXCTraceWorks();
        try {
            template = set.valueOf(templateOpt);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
        try {
            if (set.valueOf(tableOpt) == null) {
                tableType = null;
            } else {
                tableType = XCTraceTableDesc.TableType.valueOf(set.valueOf(tableOpt));
            }
        } catch (IllegalArgumentException e) {
            throw new ProfilerException(e.getMessage());
        }
        perfBinData.delete();
    }

    @Override
    protected void addMyOptions(OptionParser parser) {
        templateOpt = parser.accepts("template",
                        "Path to or name of an Instruments template. " +
                                "Use `xctrace list templates` to view available templates.")
                .withOptionalArg().ofType(String.class).defaultsTo("CPU Profiler");
        tableOpt = parser.accepts("table", "Name of the output table.")
                .withOptionalArg().ofType(String.class);
    }

    private XCTraceTableDesc.TableType chooseTable(Path profile) {
        XCTraceUtils.exportTableOfContents(profile.toAbsolutePath().toString(), perfParsedData.getAbsolutePath());
        XCTraceTableOfContentsHandler handler = new XCTraceTableOfContentsHandler();
        handler.parse(perfParsedData.file());
        List<XCTraceTableDesc> tables = handler.getSupportedTables();
        if (tables.isEmpty()) {
            throw new IllegalStateException("Profiling results does not contain table supported by this profiler.");
        }
        if (tableType != null) {
            return tables.stream()
                    .filter(t -> t.getTableType() == tableType)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Results table not found: " + tableType.tableName))
                    .getTableType();
        }
        if (tables.size() != 1) {
            throw new IllegalStateException("There are multiple supported tables in output, " +
                    "please specify which one to use using \"table\" option");
        }
        return tables.get(0).getTableType();
    }

    @Override
    protected void parseEvents() {
        Path profile = XCTraceUtils.findTraceFile(perfBinData.file().toPath());
        foundTable = chooseTable(profile);
        XCTraceUtils.exportTable(profile.toAbsolutePath().toString(), perfParsedData.getAbsolutePath(), foundTable);
    }

    @Override
    public void beforeTrial(BenchmarkParams params) {
        super.beforeTrial(params);
        if (!perfBinData.file().isDirectory() && !perfBinData.file().mkdirs()) {
            throw new IllegalStateException("Can't create folder " + perfBinData.getAbsolutePath());
        }
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        Collection<? extends Result> results = super.afterTrial(br, pid, stdOut, stdErr);
        try {
            TempFileUtils.removeDirectory(perfBinData.file().toPath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return results;
    }

    @Override
    protected PerfEvents readEvents(double skipMs, double lenMs) {
        Deduplicator<MethodDesc> dedup = new Deduplicator<>();
        Map<MethodDesc, AddressInterval> methods = new HashMap<>();
        Multiset<Long> events = new TreeMultiset<>();

        double endTimeMs = skipMs + lenMs;
        XCTraceTableHandler handler = new XCTraceTableHandler(foundTable, sample -> {
            // TODO: test
            double sampleTimeMs = sample.getTimeFromStartNs() / 1e6;
            if (sampleTimeMs < skipMs || sampleTimeMs >= endTimeMs) {
                return;
            }

            // TODO: always check only the add
            if (sample.getAddress() == 0L) {
                return;
            }
            events.add(sample.getAddress());

            // JIT sample
            if (sample.getBinary() != null) {
                String name = sample.getBinary();
                if (name.isEmpty()) {
                    // TODO
                    name = "[unknown]";
                    throw new IllegalStateException();
                }
                MethodDesc method = dedup.dedup(MethodDesc.nativeMethod(sample.getSymbol(), name));

                methods.compute(method, (key, value) -> {
                    if (value == null) {
                        return new AddressInterval(sample.getAddress());
                    }
                    value.add(sample.getAddress());
                    return value;
                });
            }
        });
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(perfParsedData.file(), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }

        IntervalMap<MethodDesc> methodMap = new IntervalMap<>();
        methods.forEach((method, addresses) -> {
            methodMap.add(method, addresses.getMin(), addresses.getMax());
        });

        Map<String, Multiset<Long>> allEvents = new TreeMap<>();
        assert requestedEventNames.size() == 1;
        allEvents.put(requestedEventNames.get(0), events);
        return new PerfEvents(requestedEventNames, allEvents, methodMap);
    }

    @Override
    protected String perfBinaryExtension() {
        return ".trace";
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return XCTraceUtils.recordCommandPrefix(perfBinData.getAbsolutePath(), template);
    }

    @Override
    public String getDescription() {
        return "macOS xctrace (Instruments) + PrintAssembly profiler";
    }
}
