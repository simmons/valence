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

package com.cafbit.valence.rfb;

import java.io.IOException;

public class RFBSecurityVNC extends RFBSecurity {

    private static final String NAME = "VNC Authentication";
    public byte getType() {
        return RFBConnection.SECURITY_VNCAUTH;
    }
    public String getTypeName() {
        return NAME;
    }

    private String password;

    public RFBSecurityVNC() {

    }

    public RFBSecurityVNC(String password) {
        if (password != null) {
            this.password = password;
        } else {
            this.password = "";
        }
    }

    public boolean perform(RFBStream stream) throws IOException {
        byte challenge[] = stream.read(16);
        byte response[] = DES.encrypt(challenge, password);
        stream.write(response);
        return true;
    }

}
