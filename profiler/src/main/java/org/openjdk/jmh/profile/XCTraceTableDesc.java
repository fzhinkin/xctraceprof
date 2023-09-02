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

import java.util.Collections;
import java.util.List;

class XCTraceTableDesc {
    public static final XCTraceTableDesc CPU_PROFILE = new XCTraceTableDesc(TableType.CPU_PROFILE);
    public static final XCTraceTableDesc TIME_PROFILE = new XCTraceTableDesc(TableType.TIME_PROFILE);

    public enum TableType {
        TIME_PROFILE("time-profile"),
        CPU_PROFILE("cpu-profile"),
        COUNTERS_PROFILE("counters-profile");

        TableType(String name) {
            tableName = name;
        }

        public final String tableName;

    }

    public enum TriggerType {
        TIME,
        PMI,
        UNKNOWN
    }

    private final TableType tableType;
    private final TriggerType triggerType;
    private final List<String> counters;
    private final String trigger;
    private final long threshold;

    public XCTraceTableDesc(TableType tableType, TriggerType triggerType, List<String> counters, String trigger, long threshold) {
        this.tableType = tableType;
        this.triggerType = triggerType;
        this.counters = counters;
        this.trigger = trigger;
        this.threshold = threshold;
    }

    public XCTraceTableDesc(TableType tableType) {
        this(tableType, TriggerType.UNKNOWN, Collections.emptyList(), "", -1);
    }

    public TableType getTableType() {
        return tableType;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public List<String> counters() {
        return counters;
    }

    public String triggerEvent() {
        return trigger;
    }

    public long triggerThreshold() {
        return threshold;
    }
}

