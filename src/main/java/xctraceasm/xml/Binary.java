package xctraceasm.xml;

public class Binary extends TraceEntry {
    private String name =  "";

    public Binary(long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
