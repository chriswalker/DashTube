/*
 * Copyright 2013-2014 That Amazing Web Ltd.
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

package com.taw.dashtube;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.taw.dashtube.model.ArrayOfLineStatus;
import com.taw.dashtube.model.LineStatus;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * Utility class mainly concerned with parsing XML responses and processing
 * results based on the user's preferences.
 */
public class DashTubeUtils {

    /** Logging tag. */
    private static final String TAG = "DashTubeUTils";

    /**
     * XML Object Parser, with default namespaces set. We get occasional namespace-related exceptions
     * due to no aliases being set for default namespaces, so provide the main service URI here
     */
    private static XmlObjectParser parser = new XmlObjectParser(new XmlNamespaceDictionary().set("", "http://webservices.lul.co.uk/"));

    /**
     * Parse the supplied XML string into a {@code ArrayOfLineStatus} object. Returns at least
     * a {@code ArrayOfLineStatus} with no status list (if there are no issues on the tube),
     * or null if we had a problem parsing.
     *
     * @param xml XML string to parse
     * @return Populated {@code ArrayOfLineStatus} object, or null if there was a parsing issue
     */
    public static ArrayOfLineStatus parseTubeLineStatusResponse(String xml) {
        try {
            return parser.parseAndClose(new StringReader(xml), ArrayOfLineStatus.class);
        } catch (IOException ioe) {
            Log.e(TAG, "Problem parsing status response", ioe);
            return null;
        }
    }

    /**
     * Filters the array of LineStatus objects down to just those required by the user, or
     * all of them if no preference has been declared. Additionally sorts the list into tube
     * name order, as the feed isn't ordered by tube name.
     *
     * @param context Context to aid in getting shared preferences
     * @param results The original results from the LineStatus API call
     * @return Filtered {@code List} of {@code LineStatus}, if filtered, or the original list otherwise
     */
    public static ArrayList<LineStatus> getFilteredResults(Context context, ArrayOfLineStatus results) {
        Collections.sort(results.status, new Comparator<LineStatus>() {
            @Override
            public int compare(LineStatus lhs, LineStatus rhs) {
                return lhs.line.name.compareToIgnoreCase(rhs.line.name);
            }
        });

        Set<String> preferredLines =  PreferenceManager.getDefaultSharedPreferences(context).getStringSet(DashTubeExtension.FAVOURITE_LINES_PREF, null);
        if (preferredLines == null || preferredLines.size() == 0) return results.status;

        // Otherwise, have favourites set - extract them
        ArrayList<LineStatus> filteredResults = new ArrayList<LineStatus>();
        for (LineStatus status : results.status) {
            if (preferredLines.contains(status.line.id)) {
                filteredResults.add(status);
            }
        }

        return filteredResults;
    }
}
