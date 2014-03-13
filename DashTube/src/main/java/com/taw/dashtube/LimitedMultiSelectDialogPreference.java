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

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.*;

/**
 * Implementation of {@code DialogPreference} that allows the user to select up to
 * five tube lines to filter results on. If the limit of five is reached, remaining
 * options in the dialog are disabled until once of the checked options is unchecked again.
 * The user's selections are stored in the 'preferred_lines' shared preference key as a
 * {@code Set<String>}.
 */
public class LimitedMultiSelectDialogPreference extends DialogPreference {

    /** Reference to the parent context. */
    private Context context;

    private TubeLineAdapter adapter;

    // TODO refactor -> move into adapter?
    private String[] lineCodes;
    private String[] lineNames;

    /**
     * Holds a list of selected tube codes. Will be used when generating the {@code ArrayList<TubeLine>}
     * required for the {@code TubeLineAdapter}, and also populated on restoring instance state.
     */
    Set<String> selectedValues = new HashSet<String>();

    /** Limit for lines that can be selected. */
    private static final int FAVOURITE_LINES_LIMIT = 5;

    public LimitedMultiSelectDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        lineNames = context.getResources().getStringArray(R.array.line_names);
        lineCodes = context.getResources().getStringArray(R.array.line_codes);

        // We are storing the prefs ourselves
        setPersistent(false);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        // Attach a ListView with our custom adapter to the dialog
        ListView listView = new ListView(context);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

//        adapter = new TubeLineAdapter(context, android.R.layout.simple_list_item_multiple_choice, generateAdapterData());
        adapter = new TubeLineAdapter(context, R.layout.tube_line_item, generateAdapterData());

        listView.setAdapter(adapter);

        builder.setView(listView);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            // persistStringSet() is not public yet, so have to resort to this instead
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(DashTubeExtension.FAVOURITE_LINES_PREF, adapter.getSelectedValues());
            editor.commit();
        }

        selectedValues.clear();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        //myState.values = adapter.getSelectedValues();
        myState.values.addAll(selectedValues);

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        selectedValues.clear();
        selectedValues.addAll(myState.values);

        super.onRestoreInstanceState(myState.getSuperState());
    }

    /**
     * Generates the array for the {@code TubeLineAdapter}, based on the list
     * of tube lines, their internal TfL codes, whether individual lines have
     * been selected as a preference and whether lines need to be disabled.
     * @return {@code ArrayList<TubeLine>} of data for the adapter
     */
    private ArrayList<TubeLine> generateAdapterData() {
        if (selectedValues.size() == 0) {
            // First show of dialog - get them from shared prefs
            selectedValues.addAll(PreferenceManager.getDefaultSharedPreferences(context).getStringSet(DashTubeExtension.FAVOURITE_LINES_PREF, new HashSet<String>()));
        }

        ArrayList<TubeLine> tubeLines = new ArrayList<TubeLine>();
        TubeLine tubeLine;
        for (int i = 0; i < lineNames.length; i ++) {
            tubeLine = new TubeLine();
            tubeLine.name = lineNames[i];
            tubeLine.code = lineCodes[i];
            if (selectedValues.contains(tubeLine.code)) {
                tubeLine.checked = true;
            }
            if (selectedValues.size() == FAVOURITE_LINES_LIMIT && !tubeLine.checked) {
                tubeLine.enabled = false;
            }
            tubeLines.add(tubeLine);
        }

        return tubeLines;
    }

    /**
     * Custom {@code ArrayAdapter} that takes in {@code TubeLine} objects; these
     * represent items in the ListView together with their states (checked or
     * disabled).
     */
    private class TubeLineAdapter extends ArrayAdapter<TubeLine> {

        /** Context for local use. */
        private Context context;
        /** Number of selections made. */
        private int numberSelected = 0;
        /** Whether we have disabled some of the checkboxes. */
        private boolean viewsDisabled = false;
        /** For inflating list views. */
        private LayoutInflater inflater = null;

        private TubeLineAdapter(Context context, int resource, List<TubeLine> objects) {
            super(context, resource, objects);

            this.context = context;

            inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            numberSelected = getSelectedValues().size();
            if (numberSelected == FAVOURITE_LINES_LIMIT) {
                viewsDisabled = true;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int pos = position;

            ViewHolder holder;

            if (convertView == null) {

                convertView = inflater.inflate(R.layout.tube_line_item, parent, false);
//                int res = getLayoutResource();
//                convertView = inflater.inflate(getLayoutResource(), parent, false);

                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.line_name);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            TubeLine tubeLine = getItem(position);

            holder.text.setText(tubeLine.name);
            holder.text.setEnabled(tubeLine.enabled);
            holder.checkBox.setChecked(tubeLine.checked);
            holder.checkBox.setEnabled(tubeLine.enabled);

            // TODO - move onclick listener out, as recreated multiple times
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox check = (CheckBox) v;
                    TubeLine tubeLine = getItem(pos);
                    tubeLine.checked = check.isChecked();
                    if (check.isChecked()) {
                        selectedValues.add(tubeLine.code);
                        numberSelected++;
                    } else {
                        selectedValues.remove(tubeLine.code);
                        numberSelected--;
                    }

                    if (numberSelected == FAVOURITE_LINES_LIMIT) {
                        disableUncheckedLines();
                    } else if (numberSelected < FAVOURITE_LINES_LIMIT && viewsDisabled) {
                        enableUncheckedLines();
                    }
                }
            });

            return convertView;
        }

        /**
         * Update the backing {@code ArrayList} setting all unchecked
         * entries to disabled. We then notify observers (i.e. the {@code ListView})
         * that the data has changed.
         */
        private void disableUncheckedLines() {
            int count = getCount();
            for (int i = 0; i < count; i ++) {
                TubeLine line = getItem(i);
                if (!line.checked) {
                    line.enabled = false;
                }
            }
            viewsDisabled = true;
            notifyDataSetChanged();
        }

        /**
         * Update the backing {@code ArrayList} setting all disabled
         * entries to enabled. We then notify observers (i.e. the {@code ListView})
         * that the data has changed.
         */
        private void enableUncheckedLines() {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                TubeLine line = getItem(i);
                if (!line.enabled) {
                    line.enabled = true;
                }
            }
            viewsDisabled = false;
            notifyDataSetChanged();
        }

        /**
         * Return a {@code Set<String>} of the selected options. Codes are stored in the Set.
         * @return {@code Set} containing line IDs of all selected options
         */
        public Set<String> getSelectedValues() {
            Set<String> values = new HashSet<String>();

            TubeLine tubeLine;
            int size = getCount();
            for (int i = 0; i < size; i++) {
                tubeLine = getItem(i);
                if (tubeLine.checked) {
                    values.add(tubeLine.code);
                }
            }

            return values;
        }
    }

    /**
     * Simple class representing data and some state for each line in the {@code ListView}
     */
    private class TubeLine {
        public String code;
        public String name;
        public boolean checked = false;
        public boolean enabled = true;
    }

    /**
     * Holder class for views in the list.
     */
    private static class ViewHolder {
        public TextView text;
        public CheckBox checkBox;
    }

    /** {@inheritDoc} */
    private static class SavedState extends BaseSavedState {
        Set<String> values = new HashSet<String>();

        public SavedState(Parcel source) {
            super(source);
            String[] strings = new String[] {};
            source.readStringArray(strings);

            values.addAll(Arrays.asList(strings));
        }

        public SavedState(Parcelable superState) {
            super(superState);

        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringArray(values.toArray(new String[values.size()]));
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
