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
import org.openjdk.jmh.util.Utils;
import org.xml.sax.SAXException;
import xctraceasm.xml.CountersProfileTableDesc;
import xctraceasm.xml.TableDesc;
import xctraceasm.xml.TableOfContentsHandler;
import xctraceasm.xml.XCTraceHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XCTraceNormProfiler implements ExternalProfiler {
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

    private Path getRunPath() {
        try (Stream<Path> files = Files.list(temporaryFolder)) {
            return files
                    //.filter(path -> path.getFileName().startsWith("Launch"))
                    .collect(Collectors.toList()).get(0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        TableDesc.TableType table = TableDesc.TableType.COUNTERS_PROFILE;
        Path traceFile = getRunPath();
        XCTraceUtils.exportTableOfContents(traceFile.toAbsolutePath().toString(), outputFile.getAbsolutePath());
        TableOfContentsHandler tocHandler = new TableOfContentsHandler();
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(outputFile.file(), tocHandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
        CountersProfileTableDesc tableDesc = (CountersProfileTableDesc) tocHandler.getSupportedTables()
                .stream()
                .filter(t -> t.getTableType() == table)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Table \"" + table.tableName +
                        "\" was not found in the trace results."));
        if (tableDesc.counters().isEmpty() && tableDesc.getTriggerType() == CountersProfileTableDesc.TriggerType.TIME) {
            throw new IllegalStateException("Results does not contain any events.");
        }
        XCTraceUtils.exportTable(traceFile.toAbsolutePath().toString(), outputFile.getAbsolutePath(), table);

        BenchmarkResultMetaData md = br.getMetadata();
        if (md == null) {
            throw new UnsupportedOperationException();
        }
        long driftMs = tocHandler.getRecordStartMs() - md.getStartTime();
        long skipNs = (ProfilerUtils.measurementDelayMs(br) - driftMs) * 1000000;
        long durationNs = ProfilerUtils.measuredTimeMs(br) * 1000000;

        double[] aggregatedEvents = new double[tableDesc.counters().size() + 1];
        long[] duration = new long[] { Long.MAX_VALUE, Long.MIN_VALUE };
        long[] samplesCount = new long[1];
        XCTraceHandler handler = new XCTraceHandler(table, sample -> {
            if (sample.getTimeFromStartNs() <= skipNs || sample.getTimeFromStartNs() > skipNs + durationNs) {
                return;
            }

            long[] counters = sample.getSamples();
            for (int i = 0; i < counters.length; i++) {
                aggregatedEvents[i] += counters[i];
            }
            aggregatedEvents[aggregatedEvents.length - 1] = sample.getWeight();
            duration[0] = Math.min(duration[0], sample.getTimeFromStartNs());
            duration[1] = Math.max(duration[1], sample.getTimeFromStartNs());
            samplesCount[0]++;
        });
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(outputFile.file(), handler);
            removeDir(temporaryFolder);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }

        long timeMs = md.getStopTime() - md.getMeasurementTime();
        if (timeMs == 0L) {
            throw new UnsupportedOperationException();
        }

        double timeSpanMs = (duration[1] - duration[0]) / 1e6;
        // TODO: validate timeSpan
        double opsThroughput = md.getMeasurementOps() / (double) timeMs;
        for (int i = 0; i < aggregatedEvents.length; i++) {
            aggregatedEvents[i] = aggregatedEvents[i] / timeSpanMs / opsThroughput;
        }

        Collection<Result<?>> results = new ArrayList<>();
        for (int i = 0; i < tableDesc.counters().size(); i++) {
            String event = tableDesc.counters().get(i);
            results.add(new ScalarResult(event, aggregatedEvents[i],
                    "#/op", AggregationPolicy.AVG));
        }
        if (tableDesc.getTriggerType() == CountersProfileTableDesc.TriggerType.PMI) {
            results.add(new ScalarResult(tableDesc.triggerEvent(),
                    aggregatedEvents[aggregatedEvents.length - 1],
                    "#/op", AggregationPolicy.AVG));
        }
        results.add(new ScalarResult("TOTAL_SAMPLES", samplesCount[0], "#", AggregationPolicy.SUM));
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

    private static void removeDir(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
