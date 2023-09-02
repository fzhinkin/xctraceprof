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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class XCTraceTableHandlerTest extends XmlTestBase {
    @Test
    public void sanityTest() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        XCTraceTableHandler handler = new XCTraceTableHandler(XCTraceTableDesc.TableType.CPU_PROFILE, sample -> {
            count.incrementAndGet();
        });

        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);
        assertEquals(584, count.get());
    }

    @Test
    public void testSamples() throws Exception {
        List<XCTraceSample> samples = new ArrayList<>();
        XCTraceTableHandler handler = new XCTraceTableHandler(XCTraceTableDesc.TableType.CPU_PROFILE, samples::add);
        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);

        XCTraceSample first = samples.get(1);
        assertEquals(465925290L, first.getTimeFromStartNs());
        assertEquals(414498L, first.getWeight());
        assertEquals(0, first.getPmcCounters().length);
        assertEquals(0x1069dcf60L, first.getAddress());
        assertEquals("a", first.getSymbol());
        assertEquals("a.out", first.getBinary());

        XCTraceSample next = samples.get(166);
        assertEquals(515200163L, next.getTimeFromStartNs());
        assertEquals(1000381L, next.getWeight());
        assertEquals(0, next.getPmcCounters().length);
        assertEquals("c", next.getSymbol());
    }

    @Test
    public void unsupportedSchema() throws Exception {
        XCTraceTableHandler handler = new XCTraceTableHandler(XCTraceTableDesc.TableType.CPU_PROFILE, sample -> fail("Expected no samples"));
        assertThrows(IllegalStateException.class, () -> {
            factory.newSAXParser().parse(openResource("counters-profile.xml"), handler);
        });
    }

    @Test
    public void parseCountersProfile() throws Exception {
        List<XCTraceSample> samples = new ArrayList<>();
        XCTraceTableHandler handler = new XCTraceTableHandler(XCTraceTableDesc.TableType.COUNTERS_PROFILE, samples::add);
        factory.newSAXParser().parse(openResource("counters-profile.xml"), handler);

        assertEquals(205, samples.size());
        XCTraceSample first = samples.get(0);
        assertEquals(434050426L, first.getTimeFromStartNs());
        assertEquals(0x10e403d73L, first.getAddress());
        assertEquals(1000000L, first.getWeight());
        assertArrayEquals(new long[]{40L, 4770L}, first.getPmcCounters());
    }

    @Test
    public void parseCountersTimeProfile() throws Exception {
        List<XCTraceSample> samples = new ArrayList<>();
        XCTraceTableHandler handler = new XCTraceTableHandler(XCTraceTableDesc.TableType.COUNTERS_PROFILE, samples::add);
        factory.newSAXParser().parse(openResource("counters-time-profile.xml"), handler);

        assertEquals(149, samples.size());
        XCTraceSample first = samples.get(0);
        assertEquals(402129330L, first.getTimeFromStartNs());
        assertEquals(0L, first.getAddress());
        assertEquals(1000000L, first.getWeight());
        assertArrayEquals(new long[]{120029L, 214575L}, first.getPmcCounters());
    }
}
