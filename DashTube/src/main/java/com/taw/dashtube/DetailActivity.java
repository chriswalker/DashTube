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

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.taw.dashtube.model.ArrayOfLineStatus;
import com.taw.dashtube.model.LineStatus;
import com.taw.dashtube.model.Tube;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dialog-styled activity that details the (possibly favourite) filtered lines
 * together with their status. This will be displayed over the user's launcher,
 * so we take some unusual (for an activity) steps of making sure it doesn't show up
 * in the recent apps list and so on; in the manifest excludeFromRecents = 'true'.
 */
public class DetailActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.detail_list_view);

        String xml = getIntent().getStringExtra(DashTubeExtension.TUBE_STATUS_XML);

        // Shouldn't ever have an empty or null list at this point
        ArrayOfLineStatus result = DashTubeUtils.parseTubeLineStatusResponse(xml);
        ArrayList<LineStatus> filteredResults = DashTubeUtils.getFilteredResults(this, result);

        setListAdapter(new DetailListAdapter(this, R.layout.detail_list_item, filteredResults));

        setupWindow();
    }



    /**
     * Handle user clicking on the dialog OK button. Finishes the activity.
     *
     * @param view The view that received the event
     */
    public void close(View view) {
        finish();
    }

    /**
     * Sets up the window - slot in the updated time as provided in the intent that starts this
     * activity, and size the dialog box width, particularly for a better appearance on 7" screens.
     * Currently we adjust the dialog width to 2/3 of the window width and 3/5 of the height
     * if we're on a 7" - 10" tablet. This means under some circumstances (e.g. only one or two lines
     * have issues) the list will have blank space below it, before the dialog OK button is
     * shown.
     *
     * TODO: Determine window height at runtime once the window is laid out, then amend height if above a threshold
     */
    private void setupWindow() {
        // Some visual tweaks - first, modify window width based on screen dp. Height handled
        // later after the adapter is populated
        WindowManager.LayoutParams params = getWindow().getAttributes();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (isSmallTablet(metrics)) {
            params.width = (metrics.widthPixels * 2 / 3);
            params.height = (metrics.heightPixels * 3 / 5);
        }
        // For phone screens, we won't adjust the window dimensions

        // Don't dim the background while the activity is displayed
        params.alpha = 1.0f;
        params.dimAmount = 0.0f;

        getWindow().setAttributes(params);

        // Set dialog title
        Set<String> preferredLines =
                PreferenceManager.getDefaultSharedPreferences(this).getStringSet(DashTubeExtension.FAVOURITE_LINES_PREF, null);
        setTitle((preferredLines != null && preferredLines.size() > 0)
                ? R.string.expanded_title_filtered
                : R.string.expanded_title);

        // Updated time text
        String updatedStr = String.format(getString(R.string.updated_at),
                getIntent().getStringExtra(DashTubeExtension.TUBE_STATUS_TIMESTAMP));

        TextView time = (TextView) findViewById(R.id.updated_at);
        time.setText(updatedStr);
    }

    /**
     * Checks if we are running on a small tablet (e.g. between 7" - 10"). See link below,
     * but anything around 600dp is generally a 7" tablet; see link for more details.
     *
     * https://developer.android.com/guide/practices/screens_support.html#DeclaringTabletLayouts
     *
     * @param metrics Display Metrics for the device
     */
    private boolean isSmallTablet(DisplayMetrics metrics) {
        float dpWidth = metrics.widthPixels / metrics.density;
        return (dpWidth >= 600 && dpWidth < 720);
    }

    /**
     * Custom adapter for displaying {@code LineStatus} objects in the details list.
     */
    private class DetailListAdapter extends ArrayAdapter<LineStatus> {

        /** Context for local use. */
        private Context context;

        /** For inflating list views. */
        private LayoutInflater inflater = null;

        /** Default constructor. */
        private DetailListAdapter(Context context, int resource, List<LineStatus> objects) {
            super(context, resource, objects);
            this.context = context;
            inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.detail_list_item, parent, false);
                holder = new ViewHolder();
                holder.line = (TextView) convertView.findViewById(R.id.tube_line);
                holder.description = (TextView) convertView.findViewById(R.id.description);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            LineStatus lineStatus = getItem(position);

            Tube tube = Tube.LINE_MAP.get(lineStatus.line.id);
            holder.line.setText(tube.getName() + " - " + lineStatus.status.description);
            holder.line.setBackgroundColor(tube.getBackgroundColour());
            holder.line.setTextColor(tube.getForegroundColour());

            holder.description.setText(lineStatus.statusDetails.trim());

            return convertView;
        }

        /**
         * Holder class for views in the list.
         */
        private  class ViewHolder {
            public TextView line;
            public TextView description;
        }
    }
}
