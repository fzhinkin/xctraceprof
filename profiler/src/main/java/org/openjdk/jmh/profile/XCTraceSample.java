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

package org.openjdk.jmh.profile;

class XCTraceSample {
    public static final String TIME_SAMPLE_TRIGGER_NAME = "TIME_MICRO_SEC";
    private static final long[] EMPTY = new long[0];

    private long timeFromStartNs = 0;
    private long weight = 0;

    private String symbol = null;

    private long address = 0;

    private String binary = null;

    private long[] samples = EMPTY;

    public void setTopFrame(long address, String symbol, String binary) {
        this.address = address;
        this.symbol = symbol;
        this.binary = binary;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    public void setTime(long time) {
        timeFromStartNs = time;
    }

    public long getTimeFromStartNs() {
        return timeFromStartNs;
    }

    public long getWeight() {
        return weight;
    }

    public long[] getPmcCounters() {
        return samples;
    }

    public void setSamples(long[] samples) {
        this.samples = samples;
    }

    public long getAddress() {
        return address;
    }

    public String getBinary() {
        return binary;
    }

    public String getSymbol() {
        return symbol;
    }
}
