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

import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.taw.dashtube.model.ArrayOfLineStatus;
import com.taw.dashtube.model.LineStatus;
import com.taw.dashtube.model.Tube;

import java.io.IOException;
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
    private GregorianCalendar closureStart;
    /** End time of tube services. */
    private GregorianCalendar closureEnd;

    /** Shared Preferences keys. */
    public static final String FAVOURITE_LINES_PREF = "favourite_lines";

    /** Key for the XML  we pass to the on-tap intent. */
    public static final String TUBE_STATUS_XML = "com.taw.dashtube.TubeStatusXML";
    /** Key for the status timestamp, also passed to the on-tap intent. */
    public static final String TUBE_STATUS_TIMESTAMP = "com.taw.dashtube.TubeStatusTimestamp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Set tube closure hours (approximate values) in UTC
        closureStart = new GregorianCalendar();
        closureStart.set(Calendar.HOUR_OF_DAY, 1);
        closureStart.set(Calendar.MINUTE, 30);

        closureEnd = new GregorianCalendar();
        closureEnd.set(Calendar.HOUR_OF_DAY, 4);
        closureEnd.set(Calendar.MINUTE, 45);
    }

    /**
     * On update, retrieves a line status update, and populates an ExtensionData object
     * ready for publication. The data object my be empty if there are no updates, or may
     * be populated with some error details if we can't get the status for some reason.
     */
    @Override
    protected void onUpdateData(int reason) {

        ExtensionData data = new ExtensionData();
        if (shouldGetUpdates()) {
            try {
                GenericUrl url = new GenericUrl(getString(R.string.line_status_api_url));

                HttpTransport transport = new NetHttpTransport();
                HttpRequest req = transport.createRequestFactory().buildGetRequest(url);

                HttpResponse rsp = req.execute();
                data = processResponse(rsp);
                rsp.disconnect();
            } catch (IOException ioe) {
                // Some kind of connection issue
                data = populateExtensionData(R.string.error_status,
                        R.string.error_status,
                        getString(R.string.error_request_expanded_body),
                        null);
            }
        }

        publishUpdate(data);
    }

    /**
     * Process the response, generating a populated {@code ExtensionData} object as appropriate.
     *
     * @param rsp the response from the status request
     * @return a popualted {@code ExtensionData} ready for publication
     */
    private ExtensionData processResponse(HttpResponse rsp) throws IOException {
        ExtensionData data = null;

        // Convert response into string - we'll send the full XML string as an extra in the on-tap
        // intent; we need to do this as DashClock does not support passing parcelables/
        // serializables in intents. Note we substring here to remove the BOM that's handily sent in
        // the response from TfL.
        String xml = rsp.parseAsString().substring(3);

        ArrayOfLineStatus result = DashTubeUtils.parseTubeLineStatusResponse(xml);

        if (result != null && result.status != null) {
            ArrayList<LineStatus> filteredResults = DashTubeUtils.getFilteredResults(this, result);

            if (filteredResults.size() > 0) {
                Intent i = new Intent(this, DetailActivity.class);
                i.putExtra(TUBE_STATUS_XML, xml);
                i.putExtra(TUBE_STATUS_TIMESTAMP, DateFormat.getTimeFormat(this).format(new Date()));

                Set<String> preferredLines =  PreferenceManager.getDefaultSharedPreferences(this).getStringSet(FAVOURITE_LINES_PREF, null);
                data = populateExtensionData(R.string.status,
                        (preferredLines != null && preferredLines.size() != 0) ? R.string.expanded_title_filtered : R.string.expanded_title,
                        generateStatusString(filteredResults, preferredLines),
                        i);
            }
        } else if (result == null) {
            // We had some kind of parsing issue; logged elsewhere
            data = populateExtensionData(R.string.error_status,
                    R.string.error_status,
                    getString(R.string.error_parsing_expanded_body),
                    null);
        }

        return data;
    }

    /**
     * Determine whether to get status updates based on:
     * 1) Whether it's December 25th - Tube is closed this day only
     * 2) It's during the night closure period
     *
     * @return True if updates should be retrieve, false otherwise
     */
    private boolean shouldGetUpdates() {
        Calendar now = Calendar.getInstance();

        // Closed on Christmas Day only
        if ((now.get(Calendar.DAY_OF_MONTH) == 25) && (now.get(Calendar.MONTH) == Calendar.DECEMBER)) {
            return false;
        }

        // Else check if we're inside the night closure window. We need ms since midnight for the current date.
        long nowMs = now.getTimeInMillis() % (24 * 60 * 60 * 1000);
        if (nowMs > closureStart.getTimeInMillis() && nowMs < closureEnd.getTimeInMillis()) {
            return false;
        }

        return true;
    }

    /**
     * Generate string for extension body - this is a list of lines with some kind of problem,
     * together with their severity. Note regarding how many lines to output - if the
     * user has opted to filter output, they will have at most 5 lines worth of info (the limit
     * dashclock places on expanded body text). If the number of {@code LineStatus} objects we
     * have is > 5, then they have not filtered, and so we will truncate output appropriately.
     *
     * @param lineStatuses {@code List<LineStatus>} of lines to filter against
     * @param preferredLines any favourites set by the user
     * @return {@code String} containing body text for the extension
     */
    private String generateStatusString(List<LineStatus> lineStatuses, Set<String> preferredLines) {
        String moreStr = getString(R.string.more_lines);
        String statusStr = getString(R.string.line_status);

        // Infrequently constructing this string, so concatenation is fine here
        String str = "";

        LineStatus lineStatus;
        for (int i = 0; i < lineStatuses.size(); i ++) {
            lineStatus = lineStatuses.get(i);
            if (lineStatus.status.isActive) {
                str += String.format(statusStr, Tube.LINE_MAP.get(lineStatus.line.id).getName(), lineStatus.status.description);
                if (i != (lineStatuses.size() - 1)) {
                    str += "\n";
                }
            }
            if (preferredLines == null || preferredLines.size() == 0) {
                if ((i + 1) == LINES_LIMIT && lineStatuses.size() > LINES_LIMIT) {
                    // No filtering by user; output the "x more" msg
                    str += String.format(moreStr, lineStatuses.size() - LINES_LIMIT);
                    break;
                }
            }
        }

        return str;
    }

    /**
     * Generate an ExtensionData object ready to be published back to the main DashClock
     * process.
     *
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
