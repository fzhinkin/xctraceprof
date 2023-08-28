package xctraceasm.xml;

public class SampleTime extends TraceEntry {
    private long timeFromStartNs = 0;

    public SampleTime(long id) {
        super(id);
    }

    public long getTimeFromStartNs() {
        return timeFromStartNs;
    }

    public void setTimeFromStartNs(long timeFromStartNs) {
        this.timeFromStartNs = timeFromStartNs;
    }
}
