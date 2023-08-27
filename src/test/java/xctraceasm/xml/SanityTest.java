package xctraceasm.xml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SanityTest {
    @Test
    public void test() {
        assertTrue("Launch_java_2023-08-27_12.54.57_FEDEDCA1.trace".endsWith(".trace"));
    }

    @Test
    public void testParse() {
        System.out.println(Long.toString(-1L, 16));
        assertEquals(-1, Long.parseUnsignedLong("ffffffffffffffff", 16));
        System.out.println(Long.parseUnsignedLong("11787cc46", 16));
    }
}
