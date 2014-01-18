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

import android.app.DialogFragment;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import java.util.*;

/**
 * Preferences activity.
 */
public class DashTubeSettingsActivity extends PreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary to reflect its new
     * value.
     */
    private static Preference.OnPreferenceChangeListener prefsListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Set<String> selections = (Set<String>) value;
            if (preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference multiPreference = (MultiSelectListPreference) preference;
                List<String> sortedNames = new ArrayList<String>();

                if (selections.size() > 0) {
                    // Translate the selected IDs into strings first, so we can sort
                    for (Iterator i = selections.iterator(); i.hasNext();) {
                        sortedNames.add(DashTubeExtension.LINE_MAP.get(i.next()));
                    }
                    Collections.sort(sortedNames);
                    String summary = TextUtils.join(", ", sortedNames);
                    preference.setSummary(summary);
                } else {
                    preference.setSummary(preference.getContext().getString(R.string.preferred_summary_none_selected));
                }

            }
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show the Home As Up
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        bindPreferenceSummaryToValue(findPreference(DashTubeExtension.PREFERRED_LINES_PREF));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_about:
                DialogFragment fragment = AboutDialogFragment.getInstance();
                fragment.show(getFragmentManager(), "About");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Binding stuff


    /**
     * Binds a preference's summary to its value. More specifically, when the preference's value is
     * changed, its summary (line of text below the preference title) is updated to reflect the
     * value. The summary is also immediately updated upon calling this method. The exact display
     * format is dependent on the type of preference.
     *
     * @see #prefsListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(prefsListener);

        // Trigger the listener immediately with the preference's
        // current value.
        prefsListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getStringSet(preference.getKey(), null));
    }
}
