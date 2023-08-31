package xctraceasm.xml;

import java.util.Objects;

public class Frame extends TraceEntry {
    private String name = "";

    private long address = 0;

    private String binary = null;

    public Frame(long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(long address) {
        this.address = address;
    }

    public String getBinary() {
        return binary;
    }

    public void setBinary(Binary binary) {
        this.binary = Objects.requireNonNull(binary, "Binary is null").getName();
    }
}
