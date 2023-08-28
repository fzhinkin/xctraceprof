package xctraceasm.xml;

import java.util.Objects;

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
