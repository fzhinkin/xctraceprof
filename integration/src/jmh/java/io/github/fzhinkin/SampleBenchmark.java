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

package io.github.fzhinkin;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.XCTraceAsmProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;

@Fork(2)
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
        String initLine = "";
        if (args.length > 0) {
            initLine = args[0];
        }
        run(initLine);
    }

    private static void run(String initLine) throws Exception {
        Options opt = new OptionsBuilder()
                .include(SampleBenchmark.class.getSimpleName())
                .addProfiler(XCTraceAsmProfiler.class, initLine)
                .build();
        Collection<RunResult> result = new Runner(opt).run();
        if (result.size() != 1) {
            throw new IllegalStateException("Expected 1 result, got " + result.size());
        }
    }
}
