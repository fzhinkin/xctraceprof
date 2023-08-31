package xctraceasm.xml;

import java.util.List;

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

