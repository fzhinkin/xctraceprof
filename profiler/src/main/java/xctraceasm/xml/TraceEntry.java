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

public abstract class TraceEntry {
    public static final String SAMPLE = "row";
    public static final String CYCLE_WEIGHT = "cycle-weight";
    public static final String WEIGHT = "weight";
    public static final String PMC_EVENT = "pmc-event";
    public static final String FRAME = "frame";
    public static final String BACKTRACE = "backtrace";
    public static final String BINARY = "binary";
    public static final String SAMPLE_TIME = "sample-time";
    public static final String PMC_EVENTS = "pmc-events";


    private final long id;

    public TraceEntry(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
