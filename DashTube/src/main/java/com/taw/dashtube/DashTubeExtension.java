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

package com.taw.dashtube;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.taw.dashtube.model.ArrayOfLineStatus;
import com.taw.dashtube.model.LineStatus;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * DashClock extension that reports on problems with London Underground services.
 */
public class DashTubeExtension extends DashClockExtension {
    /** Logging tag. */
    private static final String TAG = "DashTube";
    /** How many lines to output before we truncate output and display the more info msg. */
    private static final int LINES_LIMIT = 4;

    /** Start time of tube services. */
    private Date closureStart;
    /** End time of tube services. */
    private Date closureEnd;

    /** Shared Preferences keys. */
    public static final String PREFERRED_LINES_PREF = "preferred_lines";



    /**
     * Codes to display names map - we don't output the line names as provided to us by TfL
     * in the feed, as these are occasionally too long (e.g. 'Hammersmith and City'), so
     * this map provides line IDs to display names.
     */
    public static Map<String, String> LINE_MAP;

    @Override
    public void onCreate() {
        super.onCreate();

        String[] names = getResources().getStringArray(R.array.line_names);
        String[] codes = getResources().getStringArray(R.array.line_codes);

        LINE_MAP = new HashMap<String, String>();
        for (int i = 0; i < codes.length; i ++) {
            LINE_MAP.put(codes[i], names[i]);
        }

        // Set tube closure hours (approximate values) in UTC
        Calendar start = new GregorianCalendar();
        start.set(Calendar.HOUR_OF_DAY, 1);
        start.set(Calendar.MINUTE, 30);
        closureStart = start.getTime();

        Calendar end = new GregorianCalendar();
        end.set(Calendar.HOUR_OF_DAY, 4);
        end.set(Calendar.MINUTE, 45);
        closureEnd = end.getTime();
    }

    /**
     * On update, retrieves a line status update, and populates an ExtensionData object
     * ready for publication. The data object my be empty if there are no updates, or may
     * be populated with some error details if we can't get the status for some reason.
     */
    @Override
    protected void onUpdateData(int reason) {
        Set<String> preferredLines =  PreferenceManager.getDefaultSharedPreferences(this).getStringSet(PREFERRED_LINES_PREF, null);

        ExtensionData data = new ExtensionData();
        if (isOperating()) {
            try {
                GenericUrl url = new GenericUrl(getString(R.string.line_status_api_url));

                HttpTransport transport = new NetHttpTransport();
                HttpRequest req = transport.createRequestFactory().buildGetRequest(url);

                // Workaround - can't seem to force XmlObjectParser to use UTF-8 and
                // ignore the BOM that gets sent by the TfL feed. Do it the long way
                // around and specify the XML parser outside of the request object.
                HttpResponse rsp = req.execute();

                XmlObjectParser parser = new XmlObjectParser(new XmlNamespaceDictionary());
                ArrayOfLineStatus result = parser.parseAndClose(rsp.getContent(), Charset.forName("UTF8"), ArrayOfLineStatus.class);
                List<LineStatus> filteredResults = getFilteredResults(preferredLines, result);

                if (filteredResults.size() > 0) {
                    // Have some lines with issues
                    data = populateExtensionData(R.string.status,
                            (preferredLines != null && preferredLines.size() != 0) ? R.string.expanded_title_filtered : R.string.expanded_title,
                            generateStatusString(filteredResults),
                            new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(getString(R.string.tfl_tube_status_url))));
                }
                rsp.disconnect();
            } catch (IOException ioe) {
                // Any kind of error, display the error status/body
                data = populateExtensionData(R.string.error_status,
                        R.string.error_status,
                        getString(R.string.error_expanded_body),
                        null);
            }
        }

        publishUpdate(data);
    }

    /**
     * Filters the array of LineStatus objects down to just those required
     * by the user, or all of them if no preference has been declared.
     * @param lines The {@code Set} of lines the user wants to filter on, if any
     * @param results The original results from the LineStatus API call
     * @return Filtered {@code List} of {@code LineStatus}, if filtered, or the original list otherwise
     */
    private List<LineStatus> getFilteredResults(Set<String> lines, ArrayOfLineStatus results) {
        if (lines == null || lines.size() == 0) return results.status;

        List<LineStatus> filteredResults = new ArrayList<LineStatus>();
        for (LineStatus status : results.status) {
            if (lines.contains(status.line.id)) {
                filteredResults.add(status);
            }
        }

        return filteredResults;
    }

    /**
     * Determines whether the service is actually operating given the published
     * operating hours and days (the tube does not run on 25th December). First and
     * last train times vary widely between lines; we block out 0130 - 0445 and
     * return false if the device time falls within this period. This means we should
     * avoid most "Planned Closure" statuses for anybody checking status overnight.
     *
     * Device time is converted to UTC and checked to see if it falls between
     * {@code closureStart} and {@code closureEnd}
     * @return True if tube services are running, false otherwise.
     */
    private boolean isOperating() {
        Calendar now = new GregorianCalendar();

        // Closed on Christmas Day only
        if ((now.get(Calendar.DAY_OF_MONTH) == 25) && (now.get(Calendar.MONTH) == Calendar.DECEMBER)) {
            return false;
        }

        // Else check if we're inside the night closure window
        long nowMs = now.getTime().getTime();
        return (nowMs > closureStart.getTime() && nowMs < closureEnd.getTime()) ? false : true;
    }

    /**
     * Generate string for extension body - this is a list of lines with some kind of problem,
     * together with their severity. Note regarding how many lines to output - if the
     * user has opted to filter output, they will have at most 5 lines worth of info (the limit
     * dashclock places on expanded body text). If the number of {@code LineStatus} objects we
     * have is > 5, then they have not filtered, and so we will truncate output appropriately.
     * @param lineStatuses {@code List<LineStatus} of lines to filter against
     * @return {@code String} containing body text for the extension
     */
    private String generateStatusString(List<LineStatus> lineStatuses) {
        String moreStr = getString(R.string.more_lines);
        String statusStr = getString(R.string.line_status);

        // Infrequently constructing this string, so concatenation is fine here
        String str = "";

        LineStatus lineStatus;
        for (int i = 0; i < lineStatuses.size(); i ++) {
            lineStatus = lineStatuses.get(i);
            if (lineStatus.status.isActive) {
                str += String.format(statusStr, LINE_MAP.get(lineStatus.line.id), lineStatus.status.description);
                if (i != (lineStatuses.size() - 1)) {
                    str += "\n";
                }
            }
            if (i < LINES_LIMIT && lineStatuses.size() > 5) {
                // No filtering by user; output the "x more" msg
                str += String.format(moreStr, lineStatuses.size() - LINES_LIMIT);
                break;
            }
        }

        return str;
    }

    /**
     * Generate an ExtensionData object ready to be published back to the main DashClock
     * process.
     * @param status Resource ID of the status string to use
     * @param title Resource ID of the title string to use
     * @param body String containing body text to use
     * @param intent if supplied, allows user to click on the extension and go to TfL's web site
     * @return Fully populated ExtensionData object to publish
     */
    private ExtensionData populateExtensionData(int status, int title, String body, Intent intent) {
        ExtensionData data = new ExtensionData();
        data.visible(true)
            .icon(R.drawable.ic_extension_dashtube)
            .status(getString(status))
            .expandedTitle(getString(title))
            .expandedBody(body);
        if (intent != null) {
            data.clickIntent(intent);
        }

        return data;
    }
}
