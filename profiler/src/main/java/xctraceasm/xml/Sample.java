package xctraceasm.xml;

public class Sample {
    private static final long[] EMPTY = new long[0];

    private long timeFromStartNs = 0;
    private long weight = 0;

    private Frame topFrame = null;

    private long[] samples = EMPTY;

    public void setBacktrace(Backtrace backtrace) {
        if (!backtrace.isEmpty()) {
            this.topFrame = backtrace.getTopFrame();
        }
    }

    public void setWeight(LongHolder weight) {
        this.weight = weight.getValue();
    }

    public void setTime(LongHolder time) {
        timeFromStartNs = time.getValue();
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
        if (topFrame == null) return 0;
        return topFrame.getAddress();
    }

    public String getBinary() {
        if (topFrame == null) return null;
        return topFrame.getBinary();
    }

    public String getSymbol() {
        if (topFrame == null) return null;
        return topFrame.getName();
    }
}
