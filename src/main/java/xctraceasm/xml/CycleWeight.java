package xctraceasm.xml;

public class CycleWeight extends TraceEntry {
    private long weight = 0;

    public CycleWeight(long id) {
        super(id);
    }

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }
}
