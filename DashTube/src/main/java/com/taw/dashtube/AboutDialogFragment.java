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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * Dialog fragment for the About box.
 */
public class AboutDialogFragment extends DialogFragment {

    private static AboutDialogFragment about = null;

    public static AboutDialogFragment getInstance() {
        if (about == null) {
            about = new AboutDialogFragment();
        }
        return about;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View titleView = View.inflate(getActivity(), R.layout.about_dialog_title_view, null);
        View bodyView = View.inflate(getActivity(), R.layout.about_dialog_view, null);

        // Set up strings for URLs
        TextView txt = (TextView) bodyView.findViewById(R.id.about_dialog_text);
        txt.setText(Html.fromHtml(getString(R.string.about_dialog_text)));
        txt.setMovementMethod(LinkMovementMethod.getInstance());

        return new AlertDialog.Builder(getActivity())
                .setCustomTitle(titleView)
                .setView(bodyView)
                .setPositiveButton(getString(R.string.ok_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                .create();
    }
};
