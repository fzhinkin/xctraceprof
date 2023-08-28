package xctraceasm.xml;

import java.util.ArrayList;
import java.util.List;

public class Backtrace extends TraceEntry {
    private List<Frame> frames = new ArrayList<>();

    public Backtrace(long id) {
        super(id);
    }

    public void addFrame(Frame frame) {
        frames.add(frame);
    }

    public List<Frame> frames() {
        return frames;
    }
}
