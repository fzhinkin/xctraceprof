package org.openjdk.jmh.profile;

import org.openjdk.jmh.util.Utils;
import xctraceasm.xml.TableDesc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public final class XCTraceUtils {
    private XCTraceUtils() {
    }

    public static void exportTable(String runFile, String outputFile, TableDesc.TableType table) {
        Collection<String> out = Utils.tryWith(
                "xctrace", "export",
                "--input", runFile,
                "--output", outputFile,
                "--xpath",
                "/trace-toc/run/data/table[@schema=\"" + table.tableName + "\"]"
        );
        if (!out.isEmpty()) {
            throw new IllegalStateException(out.toString());
        }
    }

    public static void exportTableOfContents(String runFile, String outputFile) {
        Collection<String> out = Utils.tryWith(
                "xctrace", "export",
                "--input", runFile,
                "--output", outputFile,
                "--toc"
        );
        if (!out.isEmpty()) {
            throw new IllegalStateException(out.toString());
        }
    }

    public static Collection<String> recordCommandPrefix(String runFile, String template) {
        return Arrays.asList(
                "xctrace", "record", "--template", template,
                "--output", runFile,
                "--target-stdout", "-",
                "--launch", "--"
        );
    }

    public static void checkXCTraceWorks() throws ProfilerException {
        Collection<String> out = Utils.tryWith("xctrace", "version");
        if (!out.isEmpty()) {
            throw new ProfilerException(out.toString());
        }
    }
}
