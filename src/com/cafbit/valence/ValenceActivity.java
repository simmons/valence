/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/
 * 
 * Copyright (C) 2014 Alexandre Quesnel
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

import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.cafbit.valence.RFBThread.RFBThreadHandler;
import com.cafbit.valence.TouchPadView.OnTouchPadEventListener;
import com.cafbit.valence.power.WakeLockFactory;
import com.cafbit.valence.rfb.RFBKeyEvent;
import com.cafbit.valence.rfb.RFBKeyEvent.SpecialKey;
import com.cafbit.valence.rfb.RFBPointerEvent;
import com.cafbit.valence.rfb.RFBSecurity;
import com.cafbit.valence.rfb.RFBSecurityARD;
import com.cafbit.valence.rfb.RFBSecurityVNC;

//import com.cafbit.motelib.R;

public class ValenceActivity extends Activity implements OnTouchPadEventListener {
    private static final String TAG = "Valence";

    private ValenceHandler handler = new ValenceHandlerImpl();

    // TODO: consider marshaling the ValenceDevice object into the
    // activity using Parcels or some such, instead of (or in addition
    // to) using the URL approach.
    private String address;
    private int port;
    private String password;
    private boolean ard35Compatibility = false;
    private boolean macAuthentication = false;
    private String username;

    private RFBThread rfbThread;
    private boolean isRunning = false;
    private InputMethodManager inputMethodManager;
    private TouchPadView touchPadView = null;

    private ToggleButton modButton = null;
    private ToggleButton keyButton = null;
    private SpecialKey modifier = null;

    private ProximityPowerManager proximityPowerManager = null;

    //////////////////////////////////////////////////////////////////////
    // Activity lifecycle
    //////////////////////////////////////////////////////////////////////

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inputMethodManager = ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE));

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            finish();
        }
        this.address = uri.getHost();
        this.port = uri.getPort();
        this.password = uri.getQueryParameter("password");
        if ((uri.getQueryParameter("ard35Compatibility") != null) &&
                (uri.getQueryParameter("ard35Compatibility").equals("true"))) {
            this.ard35Compatibility = true;
        }
        if ((uri.getQueryParameter("macAuthentication") != null) &&
                (uri.getQueryParameter("macAuthentication").equals("true"))) {
            this.macAuthentication = true;
        }
        this.username = uri.getQueryParameter("username");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        //TouchPadView
        touchPadView = new TouchPadView(this);
        touchPadView.setOnTouchPadEvent(this);

        modButton = newModifierButton("mod");
        modButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                modButton.setChecked(false);
                if (modifier == null) {
                    view.showContextMenu();
                } else {
                    modifier = null;
                }
            }
        });
        registerForContextMenu(modButton);

        keyButton = new ToggleButton(this);
        keyButton.setText("keys");
        keyButton.setTextOn("keys");
        keyButton.setTextOff("keys");
        keyButton.setFocusable(false);
        keyButton.setFocusableInTouchMode(false);
        keyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                keyButton.setChecked(false);
                view.showContextMenu();
            }
        });
        registerForContextMenu(keyButton);

        ImageButton keyboardButton = new ImageButton(this);
        Drawable keyboardDrawable = getResources().getDrawable(R.drawable.keyboard);
        keyboardButton.setImageDrawable(keyboardDrawable);
        keyboardButton.setScaleType(ScaleType.CENTER_INSIDE);
        keyboardButton.setFocusable(false);
        keyboardButton.setFocusableInTouchMode(false);
        keyboardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                inputMethodManager.showSoftInput(touchPadView, 0);
            }
        });

        // assemble layout of buttons
        LinearLayout buttonLayout = new LinearLayout(this);
        LinearLayout rightLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.addView(modButton);
        buttonLayout.addView(keyButton);
        rightLayout.addView(keyboardButton);
        rightLayout.setGravity(Gravity.RIGHT);
        buttonLayout.addView(rightLayout, LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);

        layout.addView(touchPadView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 1));
        layout.addView(buttonLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));

        setContentView(layout);

        touchPadView.setFocusable(true);
        touchPadView.setFocusableInTouchMode(true);
        touchPadView.requestFocus();

        RFBThread savedRfbThread = (RFBThread) getLastNonConfigurationInstance();
        if (savedRfbThread != null) {
            reattachThread(savedRfbThread);
        } else {
            startThread();
        }

        if (null == proximityPowerManager) {
            proximityPowerManager = new ProximityPowerManager(
                    new WakeLockFactory(
                            (PowerManager) getSystemService(Context.POWER_SERVICE)),
                    PreferenceManager.getDefaultSharedPreferences(this));
        }
    }

    /**
     * This is called when the user resumes using the activity
     * after using other programs (and at activity creation time).
     * 
     * We don't keep the network thread running when the user is
     * not running this program in the foreground, so we use this
     * method to initialize the packet list and start the
     * network thread.
     */
    @Override
    protected void onResume() {
        super.onResume();

        isRunning = true;

        if (!rfbThread.isAlive()) {
            Log.w(TAG, "rfbThread is disconnected -- reconnect.");
            rfbThread = null;
            startThread();
        } else if (rfbThread != null) {
            RFBThreadHandler rfbHandler = rfbThread.getHandler();
            if (rfbHandler != null) {
                rfbHandler.onActivityResume();
            }
        }

        if (null != proximityPowerManager) {
            proximityPowerManager.aquire();
        }
    }

    /**
     * This is called when the user leaves the activity to run
     * another program. We stop the network thread when this
     * happens.
     */
    @Override
    protected void onPause() {
        isRunning = false;
        super.onPause();

        if (rfbThread != null) {
            RFBThreadHandler rfbHandler = rfbThread.getHandler();
            if (rfbHandler != null) {
                rfbThread.getHandler().onActivityPause();
            }
        }

        if (null != proximityPowerManager) {
            proximityPowerManager.release();
        }

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        detachThread();
        RFBThread persistentRfbThread = this.rfbThread;
        this.rfbThread = null;
        return persistentRfbThread;
    }

    @Override
    protected void onDestroy() {
        stopThread();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        finish();
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

    //////////////////////////////////////////////////////////////////////
    // context menus
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.equals(modButton)) {
            menu.setHeaderTitle("Modifier keys");
            for (int i = 0; i < RFBKeyEvent.MODIFIERS.length; i++) {
                SpecialKey key = RFBKeyEvent.MODIFIERS[i];
                menu.add(Menu.NONE, (1 << 16) | i, Menu.NONE, key.name);
            }
        } else if (v.equals(keyButton)) {
            menu.setHeaderTitle("Special keys");
            for (int i = 0; i < RFBKeyEvent.SPECIALS.length; i++) {
                SpecialKey key = RFBKeyEvent.SPECIALS[i];
                menu.add(Menu.NONE, (2 << 16) | i, Menu.NONE, key.name);
            }

        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        int major = itemId >> 16;
        int minor = itemId & 0xFFFF;

        switch (major) {
        case 1:
            // mod menu
            if ((minor >= 0) && (minor < RFBKeyEvent.MODIFIERS.length)) {
                modifier = RFBKeyEvent.MODIFIERS[minor];
                modButton.setTextOn(modifier.shortName);
                modButton.setChecked(true);
            }
            break;
        case 2:
            // key menu
            if ((minor >= 0) && (minor < RFBKeyEvent.SPECIALS.length)) {
                SpecialKey key = RFBKeyEvent.SPECIALS[minor];
                sendKey(new RFBKeyEvent(key, modifier));
            }
            break;
        }

        return true;
    }

    private ToggleButton newModifierButton(String text) {
        ToggleButton button = new ToggleButton(this);
        button.setText(text);
        button.setTextOn(text);
        button.setTextOff(text);
        button.setFocusable(false);
        button.setFocusableInTouchMode(false);
        return button;
    }

    private void startThread() {
        if (rfbThread != null) {
            Log.e(TAG, "rfbThread should be null!");
        }

        // create the appropriate RFBSecurity object for this connection
        RFBSecurity security;
        if (macAuthentication) {
            security = new RFBSecurityARD(username, password);
        } else {
            security = new RFBSecurityVNC(password);
        }

        if (port != -1) {
            rfbThread = new RFBThread(handler, address, port, security);
        } else {
            rfbThread = new RFBThread(handler, address, security);
        }
        if (ard35Compatibility) {
            rfbThread.setArd35Compatibility(true);
        }
        rfbThread.start();
        startConnectDialog();
    }

    private void stopThread() {
        if (rfbThread != null) {
            RFBThread.RFBThreadHandler handler = rfbThread.getHandler();
            if (handler != null) {
                handler.quit();
            }
            rfbThread = null;
        }
    }

    private void detachThread() {
        if (rfbThread != null) {
            RFBThread.RFBThreadHandler handler = rfbThread.getHandler();
            if (handler != null) {
                // replace our activity's handler with a stub handler while
                // the thread is detached from a valid activity.
                rfbThread.setValenceHandler(new ValenceDetachedHandler());
                // notify the thread of its detachment.
                handler.onDetach();
            }
        }
    }

    private void reattachThread(RFBThread rfbThread) {
        rfbThread.setValenceHandler(handler);
        rfbThread.getHandler().onReattach();
        this.rfbThread = rfbThread;
    }

    private ProgressDialog connectDialog;

    private void startConnectDialog() {
        connectDialog = new ProgressDialog(this);
        connectDialog.setIndeterminate(true);
        connectDialog.setMessage("Connecting to " + address);
        connectDialog.show();
        connectDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ValenceActivity.this.finish();
            }
        });
    }

    private void stopConnectDialog() {
        if (connectDialog != null) {
            connectDialog.dismiss();
        }
    }

    private final boolean isConnected() {
        if ((rfbThread != null) && (rfbThread.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if ((keyCode == KeyEvent.KEYCODE_MENU) || (keyCode == KeyEvent.KEYCODE_BACK)) {
            return super.onKeyDown(keyCode, keyEvent);
        }
        sendKey(new RFBKeyEvent(keyEvent, modifier));
        return true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent keyEvent) {
        // handle the special case of a ACTION_MULTIPLE event with key code of KEYCODE_UNKNOWN,
        // which signals a raw string of characters associated with the event.
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            String chars = keyEvent.getCharacters();
            if (chars != null) {
                for (char c : chars.toCharArray()) {
                    sendKey(new RFBKeyEvent(c, modifier));
                }
            }
            return true;
        } else {
            return super.onKeyMultiple(keyCode, repeatCount, keyEvent);
        }
    }

    /*
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
        System.out.println("********** onKeyUp() keyCode="+keyCode+" keyEvent="+keyEvent+" isCanceled="+keyEvent.isCanceled()+" isTracking="+keyEvent.isTracking());
        return super.onKeyUp(keyCode, keyEvent);
    }
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent keyEvent) {
        System.out.println("********** onKeyLongPress() keyCode="+keyCode+" keyEvent="+keyEvent);
        return super.onKeyLongPress(keyCode, keyEvent);
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        System.out.println("********** dispatchKeyEvent() keyEvent="+event);
        return super.dispatchKeyEvent(event);
    }
    @Override
    public void onUserInteraction() {
        System.out.println("********** onUserInteraction()");
        super.onUserInteraction();
    }
    */

    private void sendKey(RFBKeyEvent rfbKeyEvent) {
        if (isConnected()) {
            rfbThread.getHandler().onRFBEvent(rfbKeyEvent);
            if (modifier != null) {
                modifier = null;
                modButton.setChecked(false);
            }
        }
    }

    //
    // implement OnTouchPadEventListener
    //

    @Override
    public void onTouchPadEvent(TouchPadEvent tpe) {
        if (isConnected()) {
            // convert the TouchPadEvent to an RFBPointerEvent by
            // applying the following complex transformation to
            // overcome the fundamental TPE vs. RPE mismatch
            // inherent in the system.
            RFBPointerEvent rpe = new RFBPointerEvent();
            rpe.dt = tpe.dt;
            rpe.dx = tpe.dx;
            rpe.dy = tpe.dy;
            rpe.button1 = tpe.button1;
            rpe.button2 = tpe.button2;
            rpe.sy = tpe.sy;
            rpe.sx = tpe.sx;

            // send the RFBPointerEvent
            //tpe.debug();
            rfbThread.getHandler().onRFBEvent(rpe);
        }
    }

    //

    private boolean finishOnAlert = false;

    private void alert(String title, String message) {
        stopConnectDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.show();
        alertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                if (finishOnAlert) {
                    ValenceActivity.this.finish();
                }
            }
        });
        return;
    }

    public interface ValenceHandler {
        public void error(Throwable throwable);

        public void onConnect();

        public void onDisconnect();

        public void onAbnormalDisconnect();
    };

    public static class ValenceDetachedHandler implements ValenceHandler {
        @Override
        public void error(Throwable throwable) {
        }

        @Override
        public void onAbnormalDisconnect() {
        }

        @Override
        public void onConnect() {
        }

        @Override
        public void onDisconnect() {
        }
    };

    public class ValenceHandlerImpl extends Handler implements ValenceHandler {

        public static final int MSG_ERROR = 1;
        public static final int MSG_CONNECT = 2;
        public static final int MSG_DISCONNECT = 3;
        public static final int MSG_ABNORMAL_DISCONNECT = 4;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (!isRunning) {
                return;
            }

            switch (msg.what) {
            case MSG_ERROR:
                finishOnAlert = true;
                String text;
                if (msg.obj instanceof IOException) {
                    msg.obj = new ValenceIOException((IOException) msg.obj);
                }
                if (msg.obj instanceof UnknownHostException) {
                    text = "unknown host: " + ((Throwable) msg.obj).getMessage();
                } else {
                    text = ((Throwable) msg.obj).getMessage();
                }
                alert("Error", text);
                break;
            case MSG_CONNECT:
                stopConnectDialog();
                break;
            case MSG_DISCONNECT:
                finishOnAlert = true;
                alert("Disconnect", "The remote host closed the connection.");
                break;
            case MSG_ABNORMAL_DISCONNECT:
                finishOnAlert = true;
                alert("Disconnect", "The remote host closed the connection.");
                break;
            }
        }

        // helper methods

        @Override
        public void error(Throwable throwable) {
            sendMessage(Message.obtain(this, MSG_ERROR, throwable));
        }

        @Override
        public void onConnect() {
            sendMessage(Message.obtain(this, MSG_CONNECT));
        }

        @Override
        public void onDisconnect() {
            sendMessage(Message.obtain(this, MSG_DISCONNECT));
        }

        @Override
        public void onAbnormalDisconnect() {
            sendMessage(Message.obtain(this, MSG_ABNORMAL_DISCONNECT));
        }
    }

}
