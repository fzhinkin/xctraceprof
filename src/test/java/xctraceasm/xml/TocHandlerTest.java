package xctraceasm.xml;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TocHandlerTest extends XmlTestBase {
    private final TableOfContentsHandler handler = new TableOfContentsHandler();

    @Test
    public void parseDocumentWithoutToc() throws Exception {
        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);
        assertTrue(handler.getKdebugTables().isEmpty());
    }

    @Test
    public void parsePmcToc() throws Exception {
        factory.newSAXParser().parse(openResource("pmc-toc.xml"), handler);
        List<KdebugTableDesc> tables = handler.getKdebugTables();
        assertEquals(1, tables.size());

        KdebugTableDesc pmcTable = tables.get(0);

        assertEquals(KdebugTableDesc.TableType.PMI_SAMPLE, pmcTable.getType());
        assertEquals(1000000L, pmcTable.triggerThreshold());
        assertEquals("MEM_INST_RETIRED.ALL_LOADS", pmcTable.triggerEvent());
        assertEquals(Arrays.asList(
                "L1D_CACHE_MISS_LD", "MEM_LOAD_RETIRED.L1_HIT"
        ), pmcTable.counters());
    }

    @Test
    public void parseTimeToc() throws Exception {
        factory.newSAXParser().parse(openResource("time-toc.xml"), handler);
        List<KdebugTableDesc> tables = handler.getKdebugTables();
        assertEquals(1, tables.size());

        KdebugTableDesc timeTable = tables.get(0);

        assertEquals(KdebugTableDesc.TableType.TIME_SAMPLE, timeTable.getType());
        assertEquals(1000L, timeTable.triggerThreshold());
        assertEquals("TIME_MICRO_SEC", timeTable.triggerEvent());
        assertEquals(Arrays.asList(
                "INST_ALL", "CORE_ACTIVE_CYCLE", "INST_BRANCH"
        ), timeTable.counters());
    }

    @Test
    public void parseMixedToc() throws Exception {
        factory.newSAXParser().parse(openResource("mixed-toc.xml"), handler);
        List<KdebugTableDesc> tables = handler.getKdebugTables();
        assertEquals(2, tables.size());

        KdebugTableDesc pmcTable = tables.stream()
                .filter(t -> t.getType() == KdebugTableDesc.TableType.PMI_SAMPLE)
                .findAny()
                .get();

        assertEquals(KdebugTableDesc.TableType.PMI_SAMPLE, pmcTable.getType());
        assertEquals(1000000L, pmcTable.triggerThreshold());
        assertEquals("CORE_ACTIVE_CYCLE", pmcTable.triggerEvent());
        assertTrue(pmcTable.counters().isEmpty());

        KdebugTableDesc timeTable = tables.stream()
                .filter(t -> t.getType() == KdebugTableDesc.TableType.TIME_SAMPLE)
                .findAny()
                .get();
        assertEquals(1000L, timeTable.triggerThreshold());
        assertEquals("TIME_MICRO_SEC", timeTable.triggerEvent());
        assertEquals(Arrays.asList(
                "INST_ALL", "CORE_ACTIVE_CYCLE", "INST_BRANCH"
        ), timeTable.counters());
    }
}
