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

import org.openjdk.jmh.util.Utils;
import xctraceasm.xml.TableDesc;

import java.util.Arrays;
import java.util.Collection;

final class XCTraceUtils {
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
