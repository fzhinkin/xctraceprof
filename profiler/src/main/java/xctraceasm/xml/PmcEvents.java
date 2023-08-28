package xctraceasm.xml;

public class PmcEvents extends TraceEntry {
    private static final long[] EMPTY = new long[0];

    private long[] counters = EMPTY;

    public PmcEvents(long id) {
        super(id);
    }

    public long[] getCounters() {
        return counters;
    }

    public void setCounters(long[] counters) {
        this.counters = counters;
    }
}
