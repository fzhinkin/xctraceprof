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

public abstract class TableDesc {
    public static final TableDesc CPU_PROFILE = new CpuProfileTableDesc();
    public enum TableType {
        TIME_PROFILE("time-profile"),
        CPU_PROFILE("cpu-profile"),
        COUNTERS_PROFILE("counters-profile");

        TableType(String name) {
            tableName = name;
        }

        public final String tableName;

    }

    private final TableType tableType;

    public TableDesc(TableType tableType) {
        this.tableType = tableType;
    }

    public TableType getTableType() {
        return tableType;
    }
}

class CpuProfileTableDesc extends TableDesc {
    public CpuProfileTableDesc() {
        super(TableType.CPU_PROFILE);
    }
}

