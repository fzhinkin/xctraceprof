package xctraceasm.xml;

import java.util.Collections;
import java.util.List;

public class Sample {
    private static final long[] EMPTY = new long[0];

    private long timeFromStartNs = 0;
    private long weight = 0;

    private Backtrace backtrace = null;

    private long[] samples = EMPTY;

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

    public long[] getSamples() {
        return samples;
    }

    public void setSamples(long[] samples) {
        this.samples = samples;
    }

    public long getAddress() {
        if (backtrace == null || backtrace.frames().isEmpty()) return 0;
        return backtrace.frames().get(0).getAddress();
    }

    public String getBinary() {
        if (backtrace == null || backtrace.frames().isEmpty()) return null;
        Binary bin = backtrace.frames().get(0).getBinary();
        if (bin == null) return null;
        return bin.getName();
    }

    public String getSymbol() {
        if (backtrace == null || backtrace.frames().isEmpty()) return null;
        return backtrace.frames().get(0).getName();
    }
}
