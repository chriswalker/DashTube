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
import android.content.SharedPreferences;
import android.os.Bundle;
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
public class DashTubeSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        // Show the Home As Up
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Force an update of the summary straight away
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(this), DashTubeExtension.PREFERRED_LINES_PREF);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(DashTubeExtension.PREFERRED_LINES_PREF)) {
            Set<String> selections = (Set<String>) sharedPreferences.getStringSet(key, new HashSet<String>());
            List<String> sortedNames = new ArrayList<String>();

            Preference preference = findPreference(DashTubeExtension.PREFERRED_LINES_PREF);

            if (selections.size() > 0) {
                // Translate the selected IDs into strings first, so we can sort
                for (String selection : selections) {
                    sortedNames.add(DashTubeExtension.LINE_MAP.get(selection));
                }
                Collections.sort(sortedNames, String.CASE_INSENSITIVE_ORDER);
                String summary = TextUtils.join(", ", sortedNames);
                preference.setSummary(summary);
            } else {
                preference.setSummary(preference.getContext().getString(R.string.preferred_summary_none_selected));
            }
        }
    }
}
