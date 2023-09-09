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
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Fork(1)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 1, time = 10)
public class TimestampFiltrationTest {
    private static final String TEMPLATE = "Time Profile";
    @Benchmark
    public int dummy() {
        return 42;
    }

    public static void main(String[] args) throws Exception{
        testSamplesWithCorrection();
        testNoSamplesWithoutCorrection();
    }

    private static void testSamplesWithCorrection() throws Exception {
        Options opt = new OptionsBuilder()
                .include(TimestampFiltrationTest.class.getSimpleName())
                .addProfiler(XCTraceAsmProfiler.class, "template=" + TEMPLATE)
                .build();
        Collection<RunResult> result = new Runner(opt).run();
        if (result.size() != 1) {
            throw new IllegalStateException("Expected 1 result, got " + result.size());
        }
        Result<?> asm = result.iterator().next().getSecondaryResults().get("asm");
        long samples = TestUtils.getEvents(asm.extendedInfo());
        if (samples < 1000) {
            throw new IllegalStateException("Not enough samples: " + samples);
        }
    }

    private static void testNoSamplesWithoutCorrection() throws Exception {
        Options opt = new OptionsBuilder()
                .include(TimestampFiltrationTest.class.getSimpleName())
                .addProfiler(XCTraceAsmProfiler.class, "fixStartTime=false:template=" + TEMPLATE)
                .build();
        Collection<RunResult> result = new Runner(opt).run();
        if (result.size() != 1) {
            throw new IllegalStateException("Expected 1 result, got " + result.size());
        }
        Result<?> asm = result.iterator().next().getSecondaryResults().get("asm");
        long samples = TestUtils.getEvents(asm.extendedInfo());
        if (samples != 0) {
            throw new IllegalStateException("Too much samples: " + samples);
        }
    }
}
