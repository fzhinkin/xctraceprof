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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.TempFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class XCTraceNormProfiler implements ExternalProfiler {
    private static final XCTraceTableDesc.TableType SUPPORTED_TABLE_TYPE = XCTraceTableDesc.TableType.COUNTERS_PROFILE;
    private final String template;

    private final Path temporaryFolder;

    private final TempFile outputFile;

    public XCTraceNormProfiler() throws ProfilerException {
        this("CPU Counters");
    }

    public XCTraceNormProfiler(String initLine) throws ProfilerException {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter(XCTraceNormProfiler.class.getName()));

        OptionSpec<String> templateOpt = parser.accepts("template",
                        "Name of or path to Instruments template. " +
                                "Only templates with \"CPU Counters\" instrument are supported at the moment.")
                .withRequiredArg().ofType(String.class);

        OptionSet options = ProfilerUtils.parseInitLine(initLine, parser);
        template = options.valueOf(templateOpt);

        XCTraceUtils.checkXCTraceWorks();

        // TODO: check template exists

        try {
            temporaryFolder = Files.createTempDirectory("xctrace-run");
            outputFile = FileUtils.weakTempFile("xctrace-out.xml");
        } catch (IOException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return XCTraceUtils.recordCommandPrefix(temporaryFolder.toAbsolutePath().toString(), template);
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {
        if (!temporaryFolder.toFile().isDirectory() && !temporaryFolder.toFile().mkdirs()) {
            throw new IllegalStateException();
        }
    }

    private XCTraceTableDesc findTableDescription(XCTraceTableOfContentsHandler tocHandler) {
        XCTraceTableDesc tableDesc = tocHandler.getSupportedTables()
                .stream()
                .filter(t -> t.getTableType() == SUPPORTED_TABLE_TYPE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Table \"" + SUPPORTED_TABLE_TYPE.tableName +
                        "\" was not found in the trace results."));
        if (tableDesc.counters().isEmpty() && tableDesc.getTriggerType() == XCTraceTableDesc.TriggerType.TIME) {
            throw new IllegalStateException("Results does not contain any events.");
        }
        return tableDesc;
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        BenchmarkResultMetaData md = br.getMetadata();
        if (md == null) {
            return Collections.emptyList();
        }
        long measurementsDurationMs = md.getStopTime() - md.getMeasurementTime();
        if (measurementsDurationMs == 0L) {
            return Collections.emptyList();
        }
        double opsThroughput = md.getMeasurementOps() / (double) measurementsDurationMs;

        Path traceFile = XCTraceUtils.findTraceFile(temporaryFolder);
        XCTraceUtils.exportTableOfContents(traceFile.toAbsolutePath().toString(), outputFile.getAbsolutePath());

        XCTraceTableOfContentsHandler tocHandler = new XCTraceTableOfContentsHandler();
        tocHandler.parse(outputFile.file());
        XCTraceTableDesc tableDesc = findTableDescription(tocHandler);
        XCTraceUtils.exportTable(traceFile.toAbsolutePath().toString(), outputFile.getAbsolutePath(),
                SUPPORTED_TABLE_TYPE);

        // TODO: describe what is going on
        long startupDelayMs = tocHandler.getRecordStartMs() - md.getStartTime();
        long skipNs = (ProfilerUtils.measurementDelayMs(br) - startupDelayMs) * 1000000;
        long durationNs = ProfilerUtils.measuredTimeMs(br) * 1000000;

        AggregatedEvents aggregator = new AggregatedEvents(tableDesc);
        new XCTraceTableHandler(SUPPORTED_TABLE_TYPE, sample -> {
            if (sample.getTimeFromStartNs() <= skipNs || sample.getTimeFromStartNs() > skipNs + durationNs) {
                return;
            }

            aggregator.add(sample);
        }).parse(outputFile.file());

        try {
            TempFileUtils.removeDirectory(temporaryFolder);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (aggregator.eventsCount == 0) {
            return Collections.emptyList();
        }
        aggregator.normalizeByThroughput(opsThroughput);

        Collection<Result<?>> results = new ArrayList<>();
        for (int i = 0; i < tableDesc.counters().size(); i++) {
            String event = tableDesc.counters().get(i);
            results.add(new ScalarResult(event, aggregator.eventValues[i],
                    "#/op", AggregationPolicy.AVG));
        }
        if (tableDesc.getTriggerType() == XCTraceTableDesc.TriggerType.PMI) {
            results.add(new ScalarResult(tableDesc.triggerEvent(),
                    aggregator.eventValues[aggregator.eventValues.length - 1],
                    "#/op", AggregationPolicy.AVG));
        }
        return results;
    }

    @Override
    public boolean allowPrintOut() {
        return true;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    @Override
    public String getDescription() {
        return "XCTrace PMU counters statistics, normalized by operation count";
    }

    private static class AggregatedEvents {
        final List<String> eventNames;
        final double[] eventValues;
        long eventsCount = 0;

        long minTimestampMs = Long.MAX_VALUE;
        long maxTimestampMs = Long.MIN_VALUE;

        public AggregatedEvents(XCTraceTableDesc tableDesc) {
            List<String> names = new ArrayList<>(tableDesc.counters());
            names.add(tableDesc.triggerEvent());
            eventNames = Collections.unmodifiableList(names);
            eventValues = new double[eventNames.size()];
        }

        void add(XCTraceSample sample) {
            long[] counters = sample.getPmcCounters();
            for (int i = 0; i < counters.length; i++) {
                eventValues[i] += counters[i];
            }
            eventValues[eventValues.length - 1] = sample.getWeight();
            minTimestampMs = Math.min(minTimestampMs, sample.getTimeFromStartNs());
            maxTimestampMs = Math.max(maxTimestampMs, sample.getTimeFromStartNs());
            eventsCount++;
        }

        void normalizeByThroughput(double throughput) {
            if (maxTimestampMs == minTimestampMs) {
                throw new IllegalStateException("Min and max timestamps are the same.");
            }
            double timeSpanMs =  (maxTimestampMs - minTimestampMs) / 1e6;
            for (int i = 0; i < eventValues.length; i++) {
                eventValues[i] = eventValues[i] / timeSpanMs / throughput;
            }
        }
    }
}
