package org.openjdk.jmh.profile;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.util.*;
import org.xml.sax.SAXException;
import xctraceasm.xml.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XCTraceAsmProfiler extends AbstractPerfAsmProfiler {
    private OptionSpec<String> templateOpt;

    private OptionSpec<String> tableOpt;

    private final String template;
    private final TableDesc.TableType tableType;

    private TableDesc.TableType foundTable;

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

    public XCTraceAsmProfiler() throws ProfilerException {
        this("");
    }

    public XCTraceAsmProfiler(String initLine) throws ProfilerException {
        super(initLine, "sampled_pc");

        Collection<String> out = Utils.tryWith("xctrace", "version");
        if (!out.isEmpty()) {
            throw new ProfilerException(out.toString());
        }
        try {
            template = set.valueOf(templateOpt);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
        try {
            if (set.valueOf(tableOpt) == null) {
                tableType = null;
            } else {
                tableType = TableDesc.TableType.valueOf(set.valueOf(tableOpt));
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

    private Path getRunPath() {
        try (Stream<Path> files = Files.list(perfBinData.file().toPath())) {
            return files
                    //.filter(path -> path.getFileName().startsWith("Launch"))
                    .collect(Collectors.toList()).get(0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private TableDesc.TableType chooseTable(Path profile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("xctrace", "export",
                    "--input", profile.toAbsolutePath().toString(),
                    "--toc",
                    "--output", perfParsedData.getAbsolutePath());
            Process process = pb.start();
            process.waitFor();
            TableOfContentsHandler handler = new TableOfContentsHandler();
            SAXParserFactory.newInstance().newSAXParser().parse(perfParsedData.file(), handler);
            List<TableDesc> tables = handler.getKdebugTables();
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
        } catch (IOException | InterruptedException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void parseEvents() {
        Path profile = getRunPath();
        foundTable = chooseTable(profile);
        try {
            ProcessBuilder pb = new ProcessBuilder("xctrace", "export",
                    "--input", profile.toAbsolutePath().toString(),
                    "--xpath", "/trace-toc/run/data/table[@schema=\"" + foundTable.tableName + "\"]",
                    "--output", perfParsedData.getAbsolutePath());
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
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
            Files.walkFileTree(perfBinData.file().toPath(), new SimpleFileVisitor<Path>() {
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
        XCTraceHandler handler = new XCTraceHandler(foundTable, sample -> {
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
        if (!handler.observedCpuProfileSchema()) {
            throw new IllegalStateException("Parsed output does not contain cpu-profile table. " +
                    "Make sure you're using a template derived from the \"CPU Profiler\".");
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
        return Arrays.asList(
                "xctrace", "record", "--template", template,
                "--target-stdout", "-", "--output", perfBinData.getAbsolutePath(),
                "--launch", "--"
        );
    }

    @Override
    public String getDescription() {
        return "MacOS xctrace (Instruments) + PrintAssembly profiler";
    }
}
