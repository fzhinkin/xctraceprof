package xctraceasm.xml;

import java.util.List;

public class CountersProfileTableDesc extends TableDesc {
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
