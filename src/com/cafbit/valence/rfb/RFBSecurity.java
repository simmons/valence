package com.cafbit.valence.rfb;

import java.io.IOException;

public abstract class RFBSecurity {

    abstract public byte getType();
    abstract public String getTypeName();
    abstract public boolean perform(RFBStream stream) throws IOException;
    
}
