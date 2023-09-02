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

import java.util.Objects;

public class Frame extends TraceEntry {
    private final String name ;

    private final long address;

    private String binary = null;

    public Frame(long id, String name, long address) {
        super(id);
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public long getAddress() {
        return address;
    }

    public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = Objects.requireNonNull(binary, "Binary is null");
    }

}
