package xctraceasm.xml;

import java.util.ArrayList;
import java.util.List;

public class Backtrace extends TraceEntry {
    private Frame topFrame = null;

    public Backtrace(long id) {
        super(id);
    }

    public void addFrame(Frame frame) {
        if (topFrame != null) {
            throw new IllegalStateException("Overriding top frame");
        }
        topFrame = frame;
    }

    public boolean isEmpty() {
        return topFrame == null;
    }

    public Frame getTopFrame() {
        if (topFrame == null) {
            throw new IllegalStateException("Backtrace is empty");
        }
        return topFrame;
    }
}
