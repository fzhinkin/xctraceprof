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

import java.util.List;

public class CountersProfileTableDesc extends TableDesc {
    public enum TriggerType {
        TIME,
        PMI
    }

    private final TriggerType triggerType;
    private final List<String> counters;
    private final String trigger;
    private final long threshold;

    public CountersProfileTableDesc(TriggerType triggerType, List<String> counters, String trigger, long threshold) {
        super(TableType.COUNTERS_PROFILE);
        this.triggerType = triggerType;
        this.counters = counters;
        this.trigger = trigger;
        this.threshold = threshold;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public List<String> counters() {
        return counters;
    }

    public String triggerEvent() {
        return trigger;
    }

    public long triggerThreshold() {
        return threshold;
    }
}
