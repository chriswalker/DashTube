/*
 * Copyright 2013 That Amazing Web Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taw.dashtube.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a tube line, with various details
 * TODO: Enum a bit of overkill here, as we don't need typing, so figure out a way of encoding this as a resource, that's loadable on startup
 */
public enum Tube {
    // Note hex colours include alpha, while tube branding does not - have assumed FF for complete opacity.
    BAKERLOO("1", "Bakerloo", 0xFFFFFFFF, 0xC0996633),
    CENTRAL("2", "Central", 0xFFFFFFFF, 0xC0CC3333),
    CIRCLE("7", "Circle", 0xFF113892, 0xC0FFCC00),
    DISTRICT("9", "District", 0xFFFFFFFF, 0xC0006633),
    DLR("81", "DLR", 0xFFFFFFFF, 0xC0009999),
    HSMITH_AND_CITY("8", "Hammersmith & City", 0xFFFFFFFF, 0xC0CC9999),
    JUBILEE("4", "Jublilee",0xFFFFFFFF , 0xC0868F98),
    METROPOLITAN("11", "Metropolitan", 0xFFFFFFFF, 0xC0660066),
    NORTHERN("5", "Northern", 0xFFFFFFFF, 0xC0000000),
    OVERGROUND("82", "Overground", 0xFFFFFFFF, 0xC0E86A10), // Taken from website relaunch; official branding docs omit the Overground line
    PICADILLY("6", "Picadilly", 0xFFFFFFFF, 0xC0000099),
    VICTORIA("3", "Victoria", 0xFFFFFFFF, 0xC00099CC),
    WLOO_AND_CITY("12", "Waterloo & City", 0xFF113892, 0xC066CCCC);

    /** Line ID. */
    private String id;
    /** Line Name. */
    private String name;
    /** Foreground colour of line text. */
    private int foregroundColour;
    /** Background colour of line text. */
    private int backgroundColour;

    Tube(String id, String name, int foregroundColour, int backgroundColour) {
        this.id = id;
        this.name = name;
        this.foregroundColour = foregroundColour;
        this.backgroundColour = backgroundColour;
    }

    public static final Map<String, Tube> LINE_MAP = new HashMap<String, Tube>() {
        {
            for (Tube t : Tube.values()) {
                put(t.id, t);
            }
        }
    };

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getForegroundColour() {
        return foregroundColour;
    }

    public int getBackgroundColour() {
        return backgroundColour;
    }
}
