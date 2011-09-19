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

import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import com.cafbit.motelib.model.Device;
import com.cafbit.motelib.settings.AddDeviceActivity;
import com.cafbit.motelib.settings.DevicesActivity;
import com.cafbit.valence.device.ValenceDevice;

public class SelectionActivity extends DevicesActivity {

    // test code for short-circuiting the device selection menu.
    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, ValenceActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        Uri.Builder builder = new Uri.Builder()
            .scheme("valence")
            .encodedAuthority(Uri.encode("demo.local")+":"+Uri.encode("5900"));
        Uri uri = builder.build();
        uri.getPort();
        //System.out.println("URI: "+uri);
        intent.setData(uri);
        startActivity(intent);

    }
    */

    protected void onDeviceSelected(Device d) {
        if (! (d instanceof ValenceDevice)) {
            return;
        }
        ValenceDevice device = (ValenceDevice)d;

        Intent intent = new Intent(this, ValenceActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        Uri.Builder builder = new Uri.Builder()
            .scheme("valence")
            .encodedAuthority(Uri.encode(device.address)+":"+Uri.encode(""+device.port));
        if ((device.password != null) && (device.password.length() > 0)) {
            builder.appendQueryParameter("password", device.password);
        }
        if (device.ard35Compatibility) {
            builder.appendQueryParameter("ard35Compatibility", "true");
        }
        if (device.macAuthentication) {
            builder.appendQueryParameter("macAuthentication", "true");
        }
        if ((device.username != null) && (device.username.length() > 0)) {
            builder.appendQueryParameter("username", device.username);
        }
        Uri uri = builder.build();
        uri.getPort();
        intent.setData(uri);
        startActivity(intent);
    }

    protected void onAddNewDevice() {
        Intent intent = new Intent(this, AddDeviceActivity.class);
        intent.putExtra("device_class", "valence");
        startActivity(intent);
    }

    //////////////////////////////////////////////////////////////////////
    // options menu
    //////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsMenuHelper.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = OptionsMenuHelper.onOptionsItemSelected(this, item);
        if (result) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


}
