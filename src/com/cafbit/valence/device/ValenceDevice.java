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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.cafbit.motelib.model.Device;
import com.cafbit.motelib.model.DeviceClass;
import com.cafbit.xmlfoo.annotations.LameCrypt;

public class ValenceDevice extends Device {

    public int port = 5900;
    public String serverName;
    public String serverVersion;
    @LameCrypt
    public String password;
    public boolean ard35Compatibility = false;
    public boolean macAuthentication = false;
    public String username;

    public ValenceDevice() {
    }

    public ValenceDevice(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public InetAddress getInetAddress() {
        if (address == null) {
            return null;
        }
        InetAddress inetAddress;
        try {
            InetAddress addresses[] =
                InetAddress.getAllByName(address);
            // is this check really necessary?
            if (addresses.length == 0) {
                throw new UnknownHostException("No such host.");
            }
            inetAddress = addresses[0];
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        return inetAddress;
    }

    @Override
    public String getHeadline() {
        return serverName;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();

        String name;
        if (serverName == null) {
            name = "";
        } else if (serverName.contains("@")) {
            name = serverName.substring(0, serverName.indexOf('@')) + "@";
        } else {
            name = serverName + "@";
        }

        String version;
        if (serverVersion != null) {
            version = " (v"+serverVersion+")";
        } else {
            version = "";
        }

        sb.append(name);
        sb.append(address);
        if (port != 5900) {
            sb.append(":"+port);
        }
        sb.append(version);

        return sb.toString();
    }

    public String toString() {
        return "valence://"+address+"/";
    }

    // the superclass hashes address; we hash port
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + port;
        return result;
    }

    // the superclass compares address; we compare port
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValenceDevice other = (ValenceDevice) obj;
        if (port != other.port)
            return false;
        return true;
    }

}
