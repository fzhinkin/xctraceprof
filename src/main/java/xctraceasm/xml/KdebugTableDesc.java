package xctraceasm.xml;

import java.util.List;

public class KdebugTableDesc {
    public enum TableType {
        TIME_SAMPLE("kdebug-counters-with-time-sample"),
        PMI_SAMPLE("kdebug-counters-with-pmi-sample");

        TableType(String schema) {
            schemaName = schema;
        }

        public final String schemaName;
    }

    private final TableType type;
    private final List<String> counters;
    private final String trigger;
    private final long threshold;

    public KdebugTableDesc(TableType type, List<String> counters, String trigger, long threshold) {
        this.type = type;
        this.counters = counters;
        this.trigger = trigger;
        this.threshold = threshold;
    }

    public TableType getType() {
        return type;
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
