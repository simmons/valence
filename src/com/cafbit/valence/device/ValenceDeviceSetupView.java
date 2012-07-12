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

package com.cafbit.valence.device;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.cafbit.motelib.settings.DeviceSetupState;
import com.cafbit.motelib.settings.OnDeviceChange;
import com.cafbit.motelib.settings.OnSaveDeviceSetupState;
import com.cafbit.valence.rfb.RFBConnection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ValenceDeviceSetupView extends FrameLayout implements OnClickListener,OnSaveDeviceSetupState {

    private static final int MSG_PROBE_FAILURE = 0;
    private static final int MSG_PROBE_SUCCESS = 1;

    private Context context;
    private OnDeviceChange onDeviceChange;
    private ValenceDeviceClass deviceClass;

    private EditText addressEdit;
    private EditText portEdit;
    private EditText passwordEdit;
    private EditText nameEdit;
    private CheckBox macAuthCheckbox;
    private EditText usernameEdit;
    private CheckBox ard35CompatibilityCheckbox;

    public static class ValenceDeviceSetupState extends DeviceSetupState {
        public ValenceDevice device = null;
        public int state = 1;
        public boolean isUpdate = false;
        private ValenceDeviceClass.ProbeResult probeResult;
    }
    private ValenceDeviceSetupState state = new ValenceDeviceSetupState();

    public ValenceDeviceSetupView(Context context, ValenceDevice device, boolean isUpdate, OnDeviceChange onDeviceChange, ValenceDeviceClass deviceClass, ValenceDeviceSetupState state) {
        super(context);

        if (state != null) {
            this.state = state;
        } else {
            this.state.device = device;
            this.state.isUpdate = isUpdate;
        }
        if (this.state.device == null) {
            this.state.device = new ValenceDevice(deviceClass);
        }

        this.context = context;
        this.deviceClass = deviceClass;
        this.onDeviceChange = onDeviceChange;

        setPadding(10,10,10,10);
        transition(this.state.state);
    }

    private void createPhase1() {
        removeAllViews();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView stepLabel = new TextView(context);
        stepLabel.setText("Step 1: Server address");
        stepLabel.setTypeface(null, Typeface.BOLD);
        layout.addView(stepLabel);

        TextView addressLabel = new TextView(context);
        addressLabel.setText("Enter the IP address or hostname of the VNC server:");
        layout.addView(addressLabel);

        addressEdit = new EditText(context);
        addressEdit.setSingleLine();
        addressEdit.setHint("0.0.0.0");
        addressEdit.setText(state.device.getHostname());
        layout.addView(addressEdit);

        TextView portLabel = new TextView(context);
        portLabel.setText("Port number (default 5900):");
        layout.addView(portLabel);

        portEdit = new EditText(context);
        portEdit.setSingleLine();
        portEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (state.device.port != 0) {
            portEdit.setText(""+state.device.port);
        } else {
            portEdit.setText(""+RFBConnection.DEFAULT_PORT);
        }
        layout.addView(portEdit);

        Button button = new Button(context);
        button.setText("Next...");
        button.setOnClickListener(this);
        layout.addView(button);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        addView(scrollView);
    }

    private void createPhase2() {
        removeAllViews();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView stepLabel = new TextView(context);
        stepLabel.setText("Step 2: Server settings");
        stepLabel.setTypeface(null, Typeface.BOLD);
        layout.addView(stepLabel);

        // is mac authentication available?
        boolean supportsMacAuth = false;
        for (int i=0; i<state.probeResult.securityTypes.length; i++) {
            if (state.probeResult.securityTypes[i] == RFBConnection.SECURITY_ARD) {
                supportsMacAuth = true;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Address: "+state.device.address);
        if ((state.device.port != 0) && (state.device.port != RFBConnection.DEFAULT_PORT)) {
            sb.append(" port "+state.device.port);
        }
        sb.append("\n");
        sb.append("VNC protocol: "+state.probeResult.serverVersion.toString());
        if (state.probeResult.serverVersion.isAppleRemoteDesktop()) {
            sb.append(" (Apple Remote Desktop)");
        }
        sb.append("\n");
        if (supportsMacAuth) {
            sb.append("This server supports Mac OS X authentication.\n");
        }
        TextView deviceLabel = new TextView(context);
        deviceLabel.setText(sb.toString());
        layout.addView(deviceLabel);

        TextView passwordLabel = new TextView(context);
        passwordLabel.setText("Enter your password:");
        layout.addView(passwordLabel);

        passwordEdit = new EditText(context);
        passwordEdit.setSingleLine();
        passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEdit.setTransformationMethod(new PasswordTransformationMethod());
        passwordEdit.setText(state.device.password);
        layout.addView(passwordEdit);

        TextView nameLabel = new TextView(context);
        nameLabel.setText("Name used in the selection menu (optional):");
        layout.addView(nameLabel);

        nameEdit = new EditText(context);
        nameEdit.setSingleLine();
        nameEdit.setHint("Default: auto-detect server name");
        nameEdit.setText(state.device.serverName);
        layout.addView(nameEdit);

        if (state.probeResult.serverVersion.isAppleRemoteDesktop()) {

            // horizontal rule
            layout.addView(createHorizontalRule());

            TextView ardLabel = new TextView(context);
            ardLabel.setText("Apple Remote Desktop options");
            ardLabel.setTypeface(null, Typeface.BOLD);
            layout.addView(ardLabel);

            if (supportsMacAuth) {
                TextView macAuthLabel = new TextView(context);
                macAuthLabel.setText(
                    "Mac OS X authentication is required if you are " +
                    "connecting to a machine running Mac OS X 10.7 Lion.  (Or later updates to 10.6 Snow Leopard!)"
                );
                layout.addView(macAuthLabel);

                macAuthCheckbox = new CheckBox(context);
                macAuthCheckbox.setText("Use Mac OS X authentication?");
                macAuthCheckbox.setTextSize(TypedValue.COMPLEX_UNIT_PX, passwordLabel.getTextSize());
                macAuthCheckbox.setTypeface(passwordLabel.getTypeface());
                macAuthCheckbox.setPadding(
                    macAuthCheckbox.getPaddingLeft(),
                    10,
                    macAuthCheckbox.getPaddingRight(),
                    10
                );
                macAuthCheckbox.setChecked(state.device.macAuthentication);
                layout.addView(macAuthCheckbox);

                final TextView usernameLabel = new TextView(context);
                usernameLabel.setText("Username for Mac authentication:");
                usernameLabel.setEnabled(macAuthCheckbox.isChecked());
                layout.addView(usernameLabel);

                usernameEdit = new EditText(context);
                usernameEdit.setSingleLine();
                usernameEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                usernameEdit.setText(state.device.username);
                usernameEdit.setEnabled(macAuthCheckbox.isChecked());
                layout.addView(usernameEdit);

                // gray out the username stuff if mac auth isn't checked
                macAuthCheckbox.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(
                            CompoundButton buttonView,
                            boolean isChecked
                        ) {
                            if (isChecked) {
                                usernameLabel.setEnabled(true);
                                usernameEdit.setEnabled(true);
                            } else {
                                usernameLabel.setEnabled(false);
                                usernameEdit.setEnabled(false);
                            }
                        }
                    }
                );
            }

            // ARD v3.5 compatibility: send button-2 instead of button-3
            // on two-finger tap.
            TextView ard35CompatibilityLabel = new TextView(context);
            ard35CompatibilityLabel.setText(
                "The Apple Remote Desktop v3.5 update which Apple pushed to " +
                "Snow Leopard users in July 2011 requires the following option " +
                "to be set for right-clicks to work correctly."
            );
            layout.addView(ard35CompatibilityLabel);
            ard35CompatibilityCheckbox = new CheckBox(context);
            ard35CompatibilityCheckbox.setText(
                "Send mouse button-2 instead of button-3 on two-finger tap."
            );
            ard35CompatibilityCheckbox.setTextSize(TypedValue.COMPLEX_UNIT_PX, passwordLabel.getTextSize());
            ard35CompatibilityCheckbox.setTypeface(passwordLabel.getTypeface());
            ard35CompatibilityCheckbox.setPadding(
                ard35CompatibilityCheckbox.getPaddingLeft(),
                10,
                ard35CompatibilityCheckbox.getPaddingRight(),
                10
            );
            ard35CompatibilityCheckbox.setChecked(state.device.ard35Compatibility);
            layout.addView(ard35CompatibilityCheckbox);

            layout.addView(createHorizontalRule());
        } else {
            ard35CompatibilityCheckbox = null;
            macAuthCheckbox = null;
            usernameEdit = null;
        }

        Button button = new Button(context);
        button.setText("Next...");
        button.setOnClickListener(this);
        layout.addView(button);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        addView(scrollView);
    }

    private void createPhase3() {
        removeAllViews();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView titleLabel = new TextView(context);
        titleLabel.setText("Step 3: Confirm server settings");
        titleLabel.setTypeface(null, Typeface.BOLD);
        layout.addView(titleLabel);

        TextView descLabel = new TextView(context);
        descLabel.setText("The following VNC server was found: ");
        layout.addView(descLabel);

        StringBuilder sb = new StringBuilder();
        sb.append("Address: "+state.device.address+"\n");
        sb.append("Server name: "+state.device.serverName+"\n");
        sb.append("Server version: "+state.device.serverVersion+"\n");
        if (state.device.macAuthentication) {
            sb.append("Using Mac authentication with username \""+state.device.username+"\".\n");
        }
        if (state.device.ard35Compatibility) {
            sb.append("Using button-2 instead of button-3 for two-finger taps.\n");
        }
        TextView deviceLabel = new TextView(context);
        deviceLabel.setText(sb.toString());
        layout.addView(deviceLabel);

        Button button = new Button(context);
        if (state.isUpdate) {
            button.setText("Update this VNC server");
        } else {
            button.setText("Add this VNC server");
        }
        button.setOnClickListener(this);
        layout.addView(button);

        addView(layout);
    }

    private void transition(int state) {
        switch (state) {
        case 1:
            createPhase1();
            break;
        case 2:
            createPhase2();
            break;
        case 3:
            createPhase3();
            break;
        default:
            throw new RuntimeException("invalid state");
        }
        this.state.state = state;
    }

    @Override
    public void onClick(View view) {
        switch (state.state) {
        case 1:
            onPhase1Submit();
            break;
        case 2:
            onPhase2Submit();
            break;
        case 3:
            onPhase3Submit();
        }
    }

    private void readFields() {

        switch (state.state) {
        case 1:
            // read address
            String address = addressEdit.getText().toString().trim();
            if (address.length() == 0) {
                address = null;
            }

            // read port
            int port = 0;
            String portString = portEdit.getText().toString();
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                port = 0;
            }
            if (port == 0) {
                port = RFBConnection.DEFAULT_PORT;
            }

            state.device.address = address;
            state.device.port = port;
            break;

        case 2:
            // read password
            String password = passwordEdit.getText().toString();
            if (password.length() == 0) {
                password = null;
            }

            // read name
            String name = nameEdit.getText().toString();
            if (name.length() == 0) {
                name = null;
            }

            state.device.password = password;
            state.device.serverName = name;

            if (ard35CompatibilityCheckbox == null) {
                state.device.ard35Compatibility = false;
            } else {
                state.device.ard35Compatibility = ard35CompatibilityCheckbox.isChecked();
            }
            if (macAuthCheckbox == null) {
                state.device.macAuthentication = false;
                state.device.username = null;
            } else {
                state.device.macAuthentication = macAuthCheckbox.isChecked();
                state.device.username = usernameEdit.getText().toString();
                if (state.device.username.length() == 0) {
                    state.device.username = null;
                }
            }
            break;
        }
    }

    private void onPhase1Submit() {

        readFields();

        // validate address format
        // TODO: solution for name resolution timeouts?
        if (state.device.address == null) {
            alert("Error", "A valid IP address or hostname is required to connect to this VNC server.");
            return;
        }
        final InetAddress deviceAddress;
        if (state.device.address.equals(RFBConnection.MAGIC_DEMO_HOSTNAME)) {
            deviceAddress = null;
        } else {
            try {
                InetAddress addresses[] =
                    InetAddress.getAllByName(state.device.address);
                // is this check really necessary?
                if (addresses.length == 0) {
                    throw new UnknownHostException("No such host.");
                }
                deviceAddress = addresses[0];
            } catch (UnknownHostException e) {
                e.printStackTrace();
                alert("Error","\""+state.device.address+"\" is not a valid IP address or hostname.");
                return;
            }
        }

        probe();
    }

    private void onPhase2Submit() {
        readFields();
        if (state.device.macAuthentication) {
            if ((state.device.username == null) ||
                (state.device.username.length() == 0)) {
                alert("Error", "Mac authentication requires a username.");
                return;
            }
            if ((state.device.password == null) ||
                (state.device.password.length() == 0)) {
                alert("Error", "Mac authentication requires a password.");
                return;
            }
        }

        probe();
    }

    private void probe() {

        // probe the device
        // TODO: come up with some workable solution for connect timeouts.
        //       Maybe a timeout in the parent thread which leads to a forced Socket.close().
        final ProgressDialog progressDialog =
            ProgressDialog.show(context, "", "Connecting to the VNC server...", true, true);
        final ProbeHandler probeHandler = new ProbeHandler(progressDialog);
        final ValenceDevice device = this.state.device;
        Thread probeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ValenceDeviceClass.ProbeResult probeResult;
                    if (state.state == 1) {
                        probeResult =
                            deviceClass.probe(device, ValenceDeviceClass.PROBE_SECURITY);
                    } else {
                        probeResult =
                            deviceClass.probe(device, ValenceDeviceClass.PROBE_AUTH);
                    }
                    Message message = probeHandler.obtainMessage(MSG_PROBE_SUCCESS, probeResult);
                    probeHandler.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = probeHandler.obtainMessage(MSG_PROBE_FAILURE, e);
                    probeHandler.sendMessage(message);
                }
            }
        }, "valence-probe");
        probeThread.start();
        // success = success screen with information

        // failure = messagebox + return to setupview
    }

    private class ProbeHandler extends Handler implements OnCancelListener {
        //
        // three outcomes:
        // 1) the user cancels the dialog box
        // 2) the probe returns failure
        // 3) the probe returns success
        //
        private ProgressDialog progressDialog;
        private boolean valid = true;
        public ProbeHandler(ProgressDialog progressDialog) {
            this.progressDialog = progressDialog;
            progressDialog.setOnCancelListener(this);
        }
        @Override
        public void onCancel(DialogInterface dialog) {
            valid = false;
        }
        @Override
        public void handleMessage(Message message) {
            if (! valid) { return; }
            if (message.what == MSG_PROBE_SUCCESS) {
                ValenceDeviceClass.ProbeResult probeResult =
                    (ValenceDeviceClass.ProbeResult)message.obj;
                if (probeResult.probeType == ValenceDeviceClass.PROBE_SECURITY) {
                    onSuccessfulSecurityProbe(probeResult);
                } else {
                    onSuccessfulAuthProbe(probeResult);
                }
                progressDialog.dismiss();
            } else if (message.what == MSG_PROBE_FAILURE) {
                progressDialog.dismiss();
                String text;
                if (message.obj instanceof java.net.SocketTimeoutException) {
                    text = "The connection timed out.";
                } else {
                    text = ((Throwable)message.obj).getMessage();
                }
                alert("Connection Error", text);
            }
        }
    }

    private void onSuccessfulSecurityProbe(ValenceDeviceClass.ProbeResult probeResult) {
        this.state.device = probeResult.device;
        this.state.probeResult = probeResult;

        // transition to the next state
        transition(2);
    }

    private void onSuccessfulAuthProbe(ValenceDeviceClass.ProbeResult probeResult) {
        this.state.device = probeResult.device;
        this.state.probeResult = probeResult;

        // transition to the next state
        transition(3);
    }

    private void onPhase3Submit() {
        if (state.isUpdate) {
            onDeviceChange.onUpdateDevice(state.device);
        } else {
            onDeviceChange.onAddDevice(state.device);
        }
    }

    private void alert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("OK", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.show();
        return;
    }

    @Override
    public DeviceSetupState onSaveDeviceSetupState() {
        if (this.state.state < 3) {
            readFields();
        }
        return state;
    }

    private View createHorizontalRule() {
        // horizontal rule
        View horizontalRule = new View(context);
        LinearLayout.LayoutParams hrParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1);
        horizontalRule.setLayoutParams(hrParams);

        int color;
        try {
            TypedValue tv = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, tv, true);
            color = getResources().getColor(tv.resourceId);
        } catch (Exception e) {
            color = Color.WHITE;
        }

        horizontalRule.setBackgroundColor(color);
        hrParams.setMargins(4, 6, 4, 6);
        return horizontalRule;
    }
}
