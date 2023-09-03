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
    public void parseCpuProfile() throws Exception {
        verifyProfile(XCTraceTableDesc.TableType.CPU_PROFILE, "cpu-profile");
    }

    @Test
    public void unsupportedSchema() throws Exception {
        XCTraceTableHandler handler = new XCTraceTableHandler(XCTraceTableDesc.TableType.CPU_PROFILE, sample -> fail("Expected no samples"));
        assertThrows(IllegalStateException.class, () ->
                factory.newSAXParser().parse(openResource("counters-profile.xml"), handler));
    }

    @Test
    public void parseCountersProfile() throws Exception {
        verifyProfile(XCTraceTableDesc.TableType.COUNTERS_PROFILE, "counters-profile");
    }

    @Test
    public void parseCountersTimeProfile() throws Exception {
        verifyProfile(XCTraceTableDesc.TableType.COUNTERS_PROFILE, "counters-time-profile");
    }

    @Test
    public void parseTimeProfile() throws Exception {
        verifyProfile(XCTraceTableDesc.TableType.TIME_PROFILE, "time-profile");
    }

    private void verifyProfile(XCTraceTableDesc.TableType tableType, String profileName) throws Exception {
        List<XCTraceSample> samples = new ArrayList<>();
        XCTraceTableHandler handler = new XCTraceTableHandler(tableType, samples::add);
        factory.newSAXParser().parse(openResource(profileName + ".xml"), handler);

        List<Object[]> expectedRows = readExpectedData(profileName + ".csv");
        assertEquals(expectedRows.size(), samples.size());
        for (int idx = 0; idx < expectedRows.size(); idx++) {
            assertRowEquals(idx, expectedRows.get(idx), samples.get(idx));
        }
    }

    private void assertRowEquals(int rowIndex, Object[] expectedRow, XCTraceSample actualRow) {
        assertEquals("Timestamp for row " + rowIndex,
                ((Long) expectedRow[0]).longValue(), actualRow.getTimeFromStartNs());
        assertEquals("Weight for row " + rowIndex,
                ((Long) expectedRow[1]).longValue(), actualRow.getWeight());
        long expectedAddress = (Long) expectedRow[2];
        if (expectedAddress > 0) expectedAddress--;
        assertEquals("Address for row " + rowIndex,
                expectedAddress, actualRow.getAddress());
        assertEquals("Symbol for row " + rowIndex,
                expectedRow[3], actualRow.getSymbol());
        assertEquals("Library for row " + rowIndex,
                expectedRow[4], actualRow.getBinary());
        assertArrayEquals("PMC-counters for row " + rowIndex,
                (long[]) expectedRow[5], actualRow.getPmcCounters());
    }
}
