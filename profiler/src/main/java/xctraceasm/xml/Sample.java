/*
 * JMH Profilers based on "xctrace" utility
 * Copyright (C) 2023 Filipp Zhinkin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package xctraceasm.xml;

public class Sample {
    private static final long[] EMPTY = new long[0];

    private long timeFromStartNs = 0;
    private long weight = 0;

    private Frame topFrame = null;

    private long[] samples = EMPTY;

    public void setBacktrace(Frame backtrace) {
        topFrame = backtrace;
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
