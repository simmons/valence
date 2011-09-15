package com.cafbit.valence.rfb;

public class RFBSecurityNone extends RFBSecurity {

    private static final String NAME = "None";

    public byte getType() {
        return RFBConnection.SECURITY_NONE;
    }
    public String getTypeName() {
        return NAME;
    }
    
    public RFBSecurityNone() {}
    
    public boolean perform(RFBStream stream) {
        // nothing to do.
        return true;
    }

}
