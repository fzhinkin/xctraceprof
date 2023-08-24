package xctraceasm.xml;

import java.util.Collections;
import java.util.List;

public class Sample {
    private long timeFromStartNs = 0;
    private long weight = 0;

    private Backtrace backtrace = null;

    public void setBacktrace(Backtrace backtrace) {
        this.backtrace = backtrace;
    }

    public void setWeight(CycleWeight weight) {
        this.weight = weight.getWeight();
    }

    public void setTime(SampleTime time) {
        timeFromStartNs = time.getTimeFromStartNs();
    }

    public long getTimeFromStartNs() {
        return timeFromStartNs;
    }

    public long getWeight() {
        return weight;
    }

    public List<Frame> getBacktrace() {
        if (backtrace == null) return Collections.emptyList();
        return backtrace.frames();
    }

}
