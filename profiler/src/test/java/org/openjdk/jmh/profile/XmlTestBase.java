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

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class XmlTestBase {
    protected final SAXParserFactory factory = SAXParserFactory.newInstance();

    protected static InputStream openResource(String name) {
        InputStream stream = XmlTestBase.class.getResourceAsStream("/" + name);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + name);
        }
        return stream;
    }

    protected static List<Object[]> readExpectedData(String name) {
        InputStream stream = XmlTestBase.class.getResourceAsStream("/" + name);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + name);
        }
        List<Object[]> rows = new ArrayList<>();
        long[] empty = new long[0];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (true) {
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    break;
                }
                // line format:
                // timestamp;weight;0xAddress;symbol;name;pmc-events
                String[] partsRaw = line.split(";");
                if (partsRaw.length > 6) {
                    throw new IllegalStateException("Can't parse line: " + line);
                }
                String[] parts = Arrays.copyOf(partsRaw, 6);
                for (int idx = partsRaw.length; idx < parts.length; idx++) {
                    parts[idx] = "";
                }
                Object[] row = new Object[6];
                row[0] = Long.parseLong(parts[0]);
                row[1] = Long.parseLong(parts[1]);
                row[2] = Long.parseUnsignedLong(parts[2].substring(2), 16);
                row[3] = parts[3].isEmpty() ? null : parts[3];
                row[4] = parts[4].isEmpty() ? null : parts[4];
                if (parts[5].isEmpty()) {
                    row[5] = empty;
                } else {
                    row[5] = Arrays.stream(parts[5].split(" ")).mapToLong(Long::parseLong).toArray();
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return rows;
    }
}
