/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/
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

package com.cafbit.valence;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class OptionsMenuHelper {

    public static boolean onCreateOptionsMenu(Activity activity, Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    public static boolean onOptionsItemSelected(Activity activity, MenuItem item) {
        // Handle item selection
        int itemId = item.getItemId();
        if (itemId == R.id.about) {
            about(activity);
            return true;
        } else if (itemId == R.id.help) {
            Intent intent = new Intent(activity, HelpActivity.class);
            activity.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    private static void about(Activity activity) {
        LayoutInflater factory = LayoutInflater.from(activity);
        View view = factory.inflate(R.layout.about,null);
        TextView urlView = (TextView) view.findViewById(R.id.about_url);
        Linkify.addLinks(urlView, Linkify.WEB_URLS);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder
            .setTitle("About")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setView(view)
            .setCancelable(true)
            .setNeutralButton("OK", null);
        AlertDialog alert = builder.create();
        alert.show();
    }
}
