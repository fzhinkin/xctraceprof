package io.github.fzhinkin;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.XCTraceAsmProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.sql.Time;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Fork(1)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 10)
@Measurement(iterations = 1, time = 10)
public class SampleBenchmark {
    private static final double pi = Math.PI;

    @Benchmark
    public double benchmark() {
        return Math.log(Math.sqrt(pi) + pi);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(SampleBenchmark.class.getSimpleName())
                .addProfiler(XCTraceAsmProfiler.class)
                .build();
        Collection<RunResult> result = new Runner(opt).run();
        if (result.isEmpty()) {
            throw new IllegalStateException("Expected 1 result");
        }
        // TODO: check output's hottest region is StubRoutines::libmLog
    }
}
