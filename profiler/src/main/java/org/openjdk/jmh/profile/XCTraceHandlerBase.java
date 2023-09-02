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

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

class XCTraceHandlerBase extends DefaultHandler {
    public static final String SAMPLE = "row";
    public static final String CYCLE_WEIGHT = "cycle-weight";
    public static final String WEIGHT = "weight";
    public static final String PMC_EVENT = "pmc-event";
    public static final String PMI_EVENT = "pmi-event";
    public static final String PMI_THRESHOLD = "pmi-threshold";
    public static final String SAMPLE_RATE = "sample-rate-micro-seconds";
    public static final String FRAME = "frame";
    public static final String BACKTRACE = "backtrace";
    public static final String BINARY = "binary";
    public static final String SAMPLE_TIME = "sample-time";
    public static final String PMC_EVENTS = "pmc-events";
    public static final String ADDRESS = "addr";
    public static final String SCHEMA = "schema";
    public static final String TABLE = "table";
    public static final String START_DATE = "start-date";
    public static final String NAME = "name";
    public static final String NODE = "node";
    public static final String REF = "ref";
    public static final String ID = "id";
    public static final String TRIGGER = "trigger";

    private final StringBuilder builder = new StringBuilder();
    private boolean isNeedToParseCharacters = false;

    @Override
    public void characters(char[] ch, int start, int length) {
        if (isNeedToParseCharacters) {
            builder.append(ch, start, length);
        }
    }

    protected final String getCharacters() {
        String str = builder.toString();
        builder.setLength(0);
        return str;
    }

    protected final void setNeedParseCharacters(boolean need) {
        isNeedToParseCharacters = need;
    }

    protected final boolean isNeedToParseCharacters() {
        return isNeedToParseCharacters;
    }

    public final void parse(File file) {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(file, this);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
