package org.openjdk.jmh.profile;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.AbstractPerfAsmProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.util.*;
import org.xml.sax.SAXException;
import xctraceasm.xml.Binary;
import xctraceasm.xml.Frame;
import xctraceasm.xml.Sample;
import xctraceasm.xml.XCTraceHandler;

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

    private final String template;

    private final Path profilesDirectory;

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

    public XCTraceAsmProfiler(String initLine) throws ProfilerException {
        super(initLine, "samples");

        Collection<String> out = Utils.tryWith("sudo", "-n", "xctrace", "version");
        if (out.size() != 1 || !out.iterator().next().startsWith("xctrace version")) {
            throw new ProfilerException(out.toString());
        }
        try {
            template = set.valueOf(templateOpt);
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
        try {
            profilesDirectory = Files.createTempDirectory("xctrace-runs");
        } catch (IOException e) {
            throw new ProfilerException(e);
        }
    }

    @Override
    protected void addMyOptions(OptionParser parser) {
        templateOpt = parser.accepts("template",
                        "Path to or name of an Instruments template. " +
                                "Use `xctrace list templates` to view available templates.")
                .withOptionalArg().ofType(String.class).defaultsTo("CPU Profiler");
    }

    @Override
    protected void parseEvents() {
        try (Stream<Path> files = Files.list(profilesDirectory)) {
            Path profile = files
                    .filter(path -> path.getFileName().endsWith(".trace"))
                    .limit(1)
                    .collect(Collectors.toList()).get(0);
            ProcessBuilder pb = new ProcessBuilder("sudo", "-n", "xctrace", "export",
                    "--input", profile.toAbsolutePath().toString(),
                    "--xpath", "/trace-toc/run/data/table[@schema=\"cpu-profile\"]",
                    "--output", perfParsedData.getAbsolutePath());
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        try {
            Files.walkFileTree(profilesDirectory, new SimpleFileVisitor<Path>() {
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

        return super.afterTrial(br, pid, stdOut, stdErr);
    }

    @Override
    protected PerfEvents readEvents(double skipMs, double lenMs) {
        Deduplicator<MethodDesc> dedup = new Deduplicator<>();
        Map<MethodDesc, AddressInterval> methods = new HashMap<>();
        Multiset<Long> events = new TreeMultiset<>();

        double endTimeMs = skipMs + lenMs;
        XCTraceHandler handler = new XCTraceHandler(sample -> {
            double sampleTimeMs = sample.getTimeFromStartNs() / 1e9;
            if (sampleTimeMs < skipMs || sampleTimeMs >= endTimeMs) {
                return;
            }

            // TODO: always check only the add
            if (sample.getBacktrace().isEmpty()) {
                return;
            }
            Frame frame = sample.getBacktrace().get(0);
            events.add(frame.getAddress());

            // JIT sample
            Binary binary = frame.getBinary();
            if (binary != null) {
                String name = binary.getName();
                if (name.isEmpty()) {
                    name = "[unknown]";
                    throw new IllegalStateException();
                }
                MethodDesc method = dedup.dedup(MethodDesc.nativeMethod(frame.getName(), name));

                methods.compute(method, (key, value) -> {
                   if (value == null) {
                       return new AddressInterval(frame.getAddress());
                   }
                   value.add(frame.getAddress());
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
        if (handler.observedCpuProfileSchema()) {
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
                "sudo", "-n", "xctrace", "record", "--template", template,
                "--target-stdout", "-", "--output", profilesDirectory.toAbsolutePath().toString(),
                "--launch", "--"
        );
    }

    @Override
    public String getDescription() {
        return "MacOS xctrace (Instruments) + PrintAssembly profiler";
    }
}
