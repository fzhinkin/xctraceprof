package xctraceasm.xml;

public class LongHolder extends TraceEntry {
    private long value = 0;

    public LongHolder(long id) {
        super(id);
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
