package xctraceasm.xml;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class XCTraceHandlerTest extends XmlTestBase {
    @Test
    public void sanityTest() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        XCTraceHandler handler = new XCTraceHandler(TableDesc.TableType.CPU_PROFILE, sample -> {
            count.incrementAndGet();
        });

        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);
        assertTrue(handler.observedCpuProfileSchema());
        assertEquals(584, count.get());
    }

    @Test
    public void testSamples() throws Exception {
        List<Sample> samples = new ArrayList<>();
        XCTraceHandler handler = new XCTraceHandler(TableDesc.TableType.CPU_PROFILE, samples::add);
        factory.newSAXParser().parse(openResource("cpu-profile.xml"), handler);
        assertTrue(handler.observedCpuProfileSchema());

        Sample first = samples.get(1);
        assertEquals(465925290L, first.getTimeFromStartNs());
        assertEquals(414498L, first.getWeight());
        assertEquals(0, first.getSamples().length);
        assertEquals(0x1069dcf60L, first.getAddress());
        assertEquals("a", first.getSymbol());
        assertEquals("a.out", first.getBinary());

        Sample next = samples.get(166);
        assertEquals(515200163L, next.getTimeFromStartNs());
        assertEquals(1000381L, next.getWeight());
        assertEquals(0, next.getSamples().length);
        assertEquals("c", next.getSymbol());
    }

    @Test
    public void unsupportedSchema() throws Exception {
        XCTraceHandler handler = new XCTraceHandler(TableDesc.TableType.CPU_PROFILE, sample -> fail("Expected no samples"));
        factory.newSAXParser().parse(openResource("counters-profile.xml"), handler);
        assertFalse(handler.observedCpuProfileSchema());
    }

    @Test
    public void parseCountersProfile() throws Exception {
        List<Sample> samples = new ArrayList<>();
        XCTraceHandler handler = new XCTraceHandler(TableDesc.TableType.COUNTERS_PROFILE, samples::add);
        factory.newSAXParser().parse(openResource("counters-profile.xml"), handler);

        assertEquals(205, samples.size());
        Sample first = samples.get(0);
        assertEquals(434050426L, first.getTimeFromStartNs());
        assertEquals(0x10e403d73L, first.getAddress());
        assertEquals(1000000L, first.getWeight());
        assertArrayEquals(new long[] {40L, 4770L}, first.getSamples());
    }

    @Test
    public void parseCountersTimeProfile() throws Exception {
        List<Sample> samples = new ArrayList<>();
        XCTraceHandler handler = new XCTraceHandler(TableDesc.TableType.COUNTERS_PROFILE, samples::add);
        factory.newSAXParser().parse(openResource("counters-time-profile.xml"), handler);

        assertEquals(149, samples.size());
        Sample first = samples.get(0);
        assertEquals(402129330L, first.getTimeFromStartNs());
        assertEquals(0L, first.getAddress());
        assertEquals(1000000L, first.getWeight());
        assertArrayEquals(new long[] {120029L, 214575L}, first.getSamples());
    }
}
