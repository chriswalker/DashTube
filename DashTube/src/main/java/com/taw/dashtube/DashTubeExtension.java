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
    private static final int LINES_LIMIT = 3;

    /** Start time of tube services. */
    private Date closureStart;
    /** End time of tube services. */
    private Date closureEnd;

    /**
     * Codes to display names map - we don't output the line names as provided to us by TfL
     * in the feed, as these are occasionally too long (e.g. 'Hammersmith and City'), so
     * this map provides line IDs to display names.
     */
    private Map<Integer, String> lineMap;

    @Override
    public void onCreate() {
        super.onCreate();

        String[] names = getResources().getStringArray(R.array.line_names);
        int[] codes = getResources().getIntArray(R.array.line_codes);

        lineMap = new HashMap<Integer, String>();
        for (int i = 0; i < codes.length; i ++) {
            lineMap.put(codes[i], names[i]);
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

                if (result != null && result.status != null) {
                    // Have some lines with issues
                    data = populateExtensionData(R.string.status,
                            R.string.expanded_title,
                            generateStatusString(result),
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
     * Determines whether the service is actually operating given the published
     * operating hours and days (the tube does not run on 25th December). First and
     * last train times vary widely between lines; we block out 0130 - 0445 and
     * return false if the device time falls within this period. This means we should
     * avoid most "Planned Closure" statuses for anybody checking status overnight.
     *
     * Device time is converted to UTC and checked to see if it falls between
     * <code>closureStart</code> and <code>closureEnd</code>
     *
     *
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
        return !(nowMs > closureStart.getTime() && nowMs < closureEnd.getTime());
    }

    /**
     * Generate string for extension body - this is a list of lines with some kind of problem,
     * together with their severity.
     * @param result The list of LineStatus objects to check
     * @return String containing body text for the extension
     */
    private String generateStatusString(ArrayOfLineStatus result) {
        String moreStr = getString(R.string.more_lines);
        String statusStr = getString(R.string.line_status);

        // Infrequently constructing this string, so concatenation is fine here
        String str = "";

        LineStatus lineStatus;
        if (result != null && result.status != null) {
            for (int i = 0; i < result.status.size(); i ++) {
                if (i < (LINES_LIMIT)) {
                    lineStatus = result.status.get(i);
                    if (lineStatus.status.isActive) {
                        str += String.format(statusStr, lineMap.get(lineStatus.line.id), lineStatus.status.description);
                        if (i != (result.status.size() - 1)) {
                            str += "\n";
                        }
                    }
                } else {
                    str += String.format(moreStr, result.status.size() - LINES_LIMIT);
                    break;
                }
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
