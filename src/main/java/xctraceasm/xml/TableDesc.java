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

class CountersProfileTableDesc extends TableDesc {
    public enum TriggerType {
        TIME,
        PMI
    }

    private final TriggerType triggerType;
    private final List<String> counters;
    private final String trigger;
    private final long threshold;

    public CountersProfileTableDesc(TriggerType triggerType, List<String> counters, String trigger, long threshold) {
        super(TableType.COUNTERS_PROFILE);
        this.triggerType = triggerType;
        this.counters = counters;
        this.trigger = trigger;
        this.threshold = threshold;
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
