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

import android.content.Context;
import android.view.View;

import com.cafbit.motelib.discovery.DiscoveryManagerThread;
import com.cafbit.motelib.discovery.MDNSDiscoveryHandler;
import com.cafbit.motelib.model.Device;
import com.cafbit.motelib.model.DeviceClass;
import com.cafbit.motelib.settings.DeviceSetupState;
import com.cafbit.motelib.settings.OnDeviceChange;
import com.cafbit.netlib.NetworkManagerThread;
import com.cafbit.netlib.ReceiverThread;
import com.cafbit.valence.device.ValenceDeviceSetupView.ValenceDeviceSetupState;
import com.cafbit.valence.rfb.RFBConnection;
import com.cafbit.valence.rfb.RFBException;
import com.cafbit.valence.rfb.RFBSecurity;
import com.cafbit.valence.rfb.RFBSecurityARD;
import com.cafbit.valence.rfb.RFBSecurityVNC;
import com.cafbit.valence.rfb.Version;
import com.cafbit.xmlfoo.annotations.SingletonCode;

@SingletonCode("valence")
public class ValenceDeviceClass implements DeviceClass {

    // package-private constants
    static final String DEVICE_CODE = "valence";
    static final String DEVICE_NAME = "VNC Server";
    static final String DEVICE_DESCRIPTION = "Control your computer via VNC";
    // probe types
    static final int PROBE_SECURITY = 1;
    static final int PROBE_AUTH     = 2;

    @Override
    public String getDeviceCode() {
        return DEVICE_CODE;
    }

    @Override
    public String getDeviceName() {
        return DEVICE_NAME;
    }

    @Override
    public String getDeviceDescription() {
        return DEVICE_DESCRIPTION;
    }

    @Override
    public Class<? extends Device> getDeviceType() {
        return ValenceDevice.class;
    }

    @Override
    public View createDeviceSetupView(Context context, Device device, boolean isUpdate, OnDeviceChange onDeviceChange, DeviceSetupState deviceSetupState) {
        return new ValenceDeviceSetupView(context, (ValenceDevice) device, isUpdate, onDeviceChange, this, (ValenceDeviceSetupState) deviceSetupState);
    }

    static class ProbeResult {
        int probeType;
        ValenceDevice device;
        String serverName;
        Version serverVersion;
        byte[] securityTypes;
    }

    public ProbeResult probe(final ValenceDevice device, int probeType) throws IOException {

        // create the appropriate RFBSecurity object for this connection
        RFBSecurity security;
        if (device.macAuthentication) {
            security = new RFBSecurityARD(device.username, device.password);
        } else {
            security = new RFBSecurityVNC(device.password);
        }
// TODO: remove this...
// short-circuit mac auth
//security = new RFBSecurityVNC(device.password);

        RFBConnection conn;
        ProbeResult probeResult = new ProbeResult();
        if (device.address.equals(RFBConnection.MAGIC_DEMO_HOSTNAME)) {
            conn = new RFBConnection(device.address);
        } else {
            conn = new RFBConnection(device.address, device.port, security);
        }
        try {
            if (probeType == PROBE_SECURITY) {
                conn.probeSecurity();
            } else if (probeType == PROBE_AUTH) {
                conn.connect();
                conn.disconnect();
            }
        } catch (RFBException e) {
            throw new IOException(e.getMessage());
        }

        probeResult.probeType = probeType;
        probeResult.device = device;
        probeResult.serverName = conn.getServerName();
        probeResult.serverVersion = conn.getServerVersion();
        probeResult.securityTypes = conn.getSecurityTypes();

        device.deviceClass = this;
        if ((device.serverName == null) || (device.serverName.length()==0)) {
            device.serverName = conn.getServerName();
        }
        device.serverVersion = conn.getServerVersion().toString();

        return probeResult;
    }

    @Override
    public ReceiverThread getCustomDiscoveryReceiverThread(NetworkManagerThread networkManager) throws IOException {
        return null;
    }

    @Override
    public MDNSDiscoveryHandler getMDNSDiscoveryHandler(DiscoveryManagerThread networkManager) throws IOException {
        return new ValenceMDNSDiscoveryHandler(networkManager, this);
    }

}
