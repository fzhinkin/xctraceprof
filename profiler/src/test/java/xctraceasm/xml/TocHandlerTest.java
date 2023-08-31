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

package xctraceasm.xml;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TocHandlerTest extends XmlTestBase {
    private final TableOfContentsHandler handler = new TableOfContentsHandler();

    @Test
    public void parseDocumentWithoutToc() throws Exception {
        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);
        assertTrue(handler.getSupportedTables().isEmpty());
    }

    @Test
    public void parsePmcToc() throws Exception {
        factory.newSAXParser().parse(openResource("pmc-toc.xml"), handler);
        List<TableDesc> tables = handler.getSupportedTables();
        assertEquals(1693140580479L, handler.getRecordStartMs());
        assertEquals(1, tables.size());

        TableDesc table = tables.get(0);
        assertEquals(TableDesc.TableType.COUNTERS_PROFILE, table.getTableType());

        CountersProfileTableDesc pmcTable = (CountersProfileTableDesc) table;
        assertEquals(CountersProfileTableDesc.TriggerType.PMI, pmcTable.getTriggerType());
        assertEquals(1000000L, pmcTable.triggerThreshold());
        assertEquals("MEM_INST_RETIRED.ALL_LOADS", pmcTable.triggerEvent());
        assertEquals(Arrays.asList(
                "L1D_CACHE_MISS_LD", "MEM_LOAD_RETIRED.L1_HIT"
        ), pmcTable.counters());
    }

    @Test
    public void parseTimeToc() throws Exception {
        factory.newSAXParser().parse(openResource("time-toc.xml"), handler);
        assertEquals(1693153606998L, handler.getRecordStartMs());
        List<TableDesc> tables = handler.getSupportedTables();
        assertEquals(1, tables.size());

        TableDesc table = tables.get(0);
        assertEquals(TableDesc.TableType.COUNTERS_PROFILE, table.getTableType());

        CountersProfileTableDesc timeTable = (CountersProfileTableDesc) table;
        assertEquals(CountersProfileTableDesc.TriggerType.TIME, timeTable.getTriggerType());
        assertEquals(1000L, timeTable.triggerThreshold());
        assertEquals("TIME_MICRO_SEC", timeTable.triggerEvent());
        assertEquals(Arrays.asList(
                "INST_ALL", "CORE_ACTIVE_CYCLE", "INST_BRANCH"
        ), timeTable.counters());
    }

    @Test
    public void parseMixedToc() throws Exception {
        factory.newSAXParser().parse(openResource("mixed-toc.xml"), handler);
        assertEquals(1693153762702L, handler.getRecordStartMs());
        List<TableDesc> tables = handler.getSupportedTables();
        assertEquals(2, tables.size());

        assertTrue(tables.stream().anyMatch(t -> t.getTableType() == TableDesc.TableType.COUNTERS_PROFILE));
        assertTrue(tables.stream().anyMatch(t -> t.getTableType() == TableDesc.TableType.CPU_PROFILE));
    }

    @Test
    public void parseCpuProfileToc() throws Exception {
        factory.newSAXParser().parse(openResource("cpu-prof-toc.xml"), handler);
        assertEquals(1693158302632L, handler.getRecordStartMs());
        List<TableDesc> tables = handler.getSupportedTables();
        assertEquals(1, tables.size());
        assertEquals(TableDesc.TableType.CPU_PROFILE, tables.get(0).getTableType());
    }
}
