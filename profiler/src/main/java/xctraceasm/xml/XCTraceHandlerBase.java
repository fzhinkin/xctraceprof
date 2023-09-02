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

import org.xml.sax.helpers.DefaultHandler;

public class XCTraceHandlerBase extends DefaultHandler {
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
}
