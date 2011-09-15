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
