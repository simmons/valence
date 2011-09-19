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

/**
 *
 */
package com.cafbit.valence.rfb;

public class Version extends RFBMessage {

    public int major;
    public int minor;

    public Version(int version) {
        this.major = (version >> 8) & 0xFF;
        this.minor = version & 0xFF;
    }

    public Version(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public Version(byte[] buffer) throws RFBException {
        if (buffer.length != 12) {
            throw new RFBException("version buffer must be 12 bytes");
        }
        if (buffer[0] != 'R' || buffer[1] != 'F' || buffer[2] != 'B' || buffer[3] != ' ' || buffer[7] != '.' || buffer[11] != '\n') {
            throw new RFBException("bad magic from server.");
        }
        try {
            this.major = Integer.parseInt(new String(buffer, 4, 3));
            this.minor = Integer.parseInt(new String(buffer, 8, 3));
        } catch (NumberFormatException e) {
            throw new RFBException("invalid version from server", e);
        }
    }

    @Override
    public byte[] getBytes() {
        byte[] buffer = new byte[12];
        buffer[0] = 'R';
        buffer[1] = 'F';
        buffer[2] = 'B';
        buffer[3] = ' ';
        buffer[4] = (byte) (0x30 + major%1000/100);
        buffer[5] = (byte) (0x30 + major%100/10);
        buffer[6] = (byte) (0x30 + major%10);
        buffer[7] = '.';
        buffer[8] = (byte) (0x30 + minor%1000/100);
        buffer[9] = (byte) (0x30 + minor%100/10);
        buffer[10] = (byte) (0x30 + minor%10);
        buffer[11] = '\n';
        return buffer;
    }

    public int asInt() {
        return major<<8 | minor;
    }

    public String toString() {
        return ""+major+'.'+minor;
    }

    /**
     * Mac OS's built-in VNC server (Apple Remote Desktop) reports a
     * non-standard version number that breaks the protocol if not
     * taken into considerations.
     */
    public boolean isAppleRemoteDesktop() {
        if (major==3 && minor==889) {
            return true;
        } else {
            return false;
        }
    }
}