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
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
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
    private CheckBox ard35CompatibilityCheckbox;
    
    public static class ValenceDeviceSetupState extends DeviceSetupState {
        public ValenceDevice device = null;
        public int state = 1;
        public boolean isUpdate = false;
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
        
        TextView addressLabel = new TextView(context);
        addressLabel.setText("Enter the IP address or hostname of the VNC server:");
        layout.addView(addressLabel);
        
        addressEdit = new EditText(context);
        addressEdit.setSingleLine();
        addressEdit.setHint("0.0.0.0");
        if (state.device != null) {
            addressEdit.setText(state.device.getHostname());
        }
        layout.addView(addressEdit);

        TextView portLabel = new TextView(context);
        portLabel.setText("Port number (default 5900):");
        layout.addView(portLabel);

        portEdit = new EditText(context);
        portEdit.setSingleLine();
        portEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (state.device != null) {
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
        
        TextView passwordLabel = new TextView(context);
        passwordLabel.setText("Enter your password:");
        layout.addView(passwordLabel);

        passwordEdit = new EditText(context);
        passwordEdit.setSingleLine();
        passwordEdit.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEdit.setTransformationMethod(new PasswordTransformationMethod());
        if (state.device != null) {
            passwordEdit.setText(state.device.password);
        }
        layout.addView(passwordEdit);
        
        TextView nameLabel = new TextView(context);
        nameLabel.setText("Name used in the selection menu (optional):");
        layout.addView(nameLabel);

        nameEdit = new EditText(context);
        nameEdit.setSingleLine();
        nameEdit.setHint("Default: auto-detect server name");
        if (state.device != null) {
            nameEdit.setText(state.device.serverName);
        }
        layout.addView(nameEdit);

        ard35CompatibilityCheckbox = new CheckBox(context);
        ard35CompatibilityCheckbox.setText("Apple Remote Desktop v3.5 compatibility (needed only to make right-clicks work on Macs after the July 2011 ARD update)");
        ard35CompatibilityCheckbox.setTextSize(TypedValue.COMPLEX_UNIT_PX, passwordLabel.getTextSize());
        ard35CompatibilityCheckbox.setTypeface(passwordLabel.getTypeface());
        ard35CompatibilityCheckbox.setPadding(
            ard35CompatibilityCheckbox.getPaddingLeft(),
            10,
            ard35CompatibilityCheckbox.getPaddingRight(),
            10
        );
        if (state.device != null) {
            ard35CompatibilityCheckbox.setChecked(state.device.ard35Compatibility);
        }
        layout.addView(ard35CompatibilityCheckbox);

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
        titleLabel.setText("VNC server found!");
        layout.addView(titleLabel);

        TextView descLabel = new TextView(context);
        descLabel.setText("The following VNC server was found: ");
        layout.addView(descLabel);

        StringBuilder sb = new StringBuilder();
        sb.append("Address: "+state.device.address+"\n");
        sb.append("Server name: "+state.device.serverName+"\n");
        sb.append("Server version: "+state.device.serverVersion+"\n");
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
            state.device.ard35Compatibility = ard35CompatibilityCheckbox.isChecked();
            break;
        }
        
        // update state
        if (state.device == null) {
            state.device = new ValenceDevice(deviceClass);
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
                alert("Connection Error", ((Throwable)message.obj).getMessage());
            }
        }       
    }
    
    private void onSuccessfulSecurityProbe(ValenceDeviceClass.ProbeResult probeResult) {
        this.state.device = probeResult.device;
        System.out.println("security types: ");
        for (byte type : probeResult.securityTypes) {
            System.out.printf("    %02X\n", type);
        }
        // transition to the next state
        transition(2);
    }

    private void onSuccessfulAuthProbe(ValenceDeviceClass.ProbeResult probeResult) {
        this.state.device = probeResult.device;
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
    
}
