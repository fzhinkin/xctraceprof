package xctraceasm.xml;

import org.junit.Test;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class XCTraceHandlerTest {
    private final SAXParserFactory factory = SAXParserFactory.newInstance();

    private static InputStream openProfile(String name) {
        InputStream stream = XCTraceHandlerTest.class.getResourceAsStream("/" + name);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + name);
        }
        return stream;
    }

    @Test
    public void sanityTest() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        XCTraceHandler handler = new XCTraceHandler(sample -> {
            count.incrementAndGet();
        });

        factory.newSAXParser().parse(openProfile("cpu-profile.xml"), handler);
        assertTrue(handler.observedCpuProfileSchema());
        assertEquals(584, count.get());
    }

    @Test
    public void testSamples() throws Exception {
        List<Sample> samples = new ArrayList<>();
        XCTraceHandler handler = new XCTraceHandler(samples::add);
        factory.newSAXParser().parse(openProfile("cpu-profile.xml"), handler);
        assertTrue(handler.observedCpuProfileSchema());

        Sample first = samples.get(1);
        assertEquals(465925290L, first.getTimeFromStartNs());
        assertEquals(414498L, first.getWeight());
        List<Frame> firstBacktrace = first.getBacktrace();
        assertEquals(3, firstBacktrace.size());
        assertEquals(0x1069dcf61L, firstBacktrace.get(0).getAddress());
        assertEquals("a", firstBacktrace.get(0).getName());
        assertEquals("a.out", firstBacktrace.get(0).getBinary().getName());

        Sample next = samples.get(166);
        assertEquals(515200163L, next.getTimeFromStartNs());
        assertEquals(1000381L, next.getWeight());
        assertEquals(Arrays.asList("c", "b", "a"),
                next.getBacktrace().stream().map(Frame::getName).limit(3).collect(Collectors.toList()));
    }

    @Test
    public void unsupportedTraceType() throws Exception {
        XCTraceHandler handler = new XCTraceHandler(sample -> fail("Expected no samples"));
        factory.newSAXParser().parse(openProfile("counters-profile.xml"), handler);
        assertFalse(handler.observedCpuProfileSchema());
    }
}
