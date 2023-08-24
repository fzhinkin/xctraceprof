package xctraceasm.xml;

import java.util.Objects;

public abstract class TraceEntry {
    public static final String SAMPLE = "row";
    public static final String CYCLE_WEIGHT = "cycle-weight";
    public static final String FRAME = "frame";
    public static final String BACKTRACE = "backtrace";
    public static final String BINARY = "binary";
    public static final String SAMPLE_TIME = "sample-time";


    private final long id;

    public TraceEntry(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
