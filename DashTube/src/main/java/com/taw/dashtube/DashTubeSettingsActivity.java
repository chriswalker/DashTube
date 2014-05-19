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
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import com.taw.dashtube.model.Tube;

import java.util.*;

/**
 * Preferences activity.
 */
public class DashTubeSettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DialogFragment fragment;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_changelog:
                fragment = ChangelogDialogFragment.getInstance();
                fragment.show(getFragmentManager(), "Changelog");
                return true;
            case R.id.menu_about:
                fragment = AboutDialogFragment.getInstance();
                fragment.show(getFragmentManager(), "About");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Initialise the preferences activity from XML, and also bind the on change listener to
     * the various preferences.
     */
    private void setupPreferences() {
        addPreferencesFromResource(R.xml.settings);

        Preference favourites = findPreference(DashTubeExtension.FAVOURITE_LINES_PREF);
        favourites.setOnPreferenceChangeListener(listener);
        listener.onPreferenceChange(favourites,
                PreferenceManager
                        .getDefaultSharedPreferences(favourites.getContext())
                        .getStringSet(favourites.getKey(), new HashSet<String>()));
    }

    /**
     * A preference value change listener that updates the preference's summary to reflect its new
     * value.
     */
    private static Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            if (preference instanceof LimitedMultiSelectDialogPreference) {
                Set<String> selections = (Set<String>) value;
                List<String> sortedNames = new ArrayList<String>();

                if (selections.size() > 0) {
                    // Translate the selected IDs into strings first, so we can sort
                    for (String selection : selections) {
                        sortedNames.add(Tube.LINE_MAP.get(selection).getName());
                    }
                    Collections.sort(sortedNames, String.CASE_INSENSITIVE_ORDER);
                    String summary = TextUtils.join(", ", sortedNames);
                    preference.setSummary(summary);
                } else {
                    preference.setSummary(preference.getContext().getString(R.string.favourite_lines_summary_none_selected));
                }
            }

            return true;
        }
    };
}
