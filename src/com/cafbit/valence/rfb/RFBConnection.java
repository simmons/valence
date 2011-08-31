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
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.FloatMath;

public class RFBConnection {

    public static final byte SECURITY_INVALID = 0x00;
    public static final byte SECURITY_NONE    = 0x01;
    public static final byte SECURITY_VNCAUTH = 0x02;
    
    public static final int DEFAULT_PORT = 5900;
    public static final String MAGIC_DEMO_HOSTNAME = "demo.local";
    private static final int TIMEOUT = 4000;
    private static final int MAX_VERSION = 0x0308;
    private static final int MIN_VERSION = 0x0303;

    private String address;
    private InetAddress inetAddress;
    private int port;
    private String password;
    private Socket socket = null;
    private RFBStream stream = null;
    private Version serverVersion;
    private String serverName;
    private int width;
    private int height;
    private float pointerX;
    private float pointerY;
    private boolean ard35Compatibility = false;

    public RFBConnection(String address, int port, String password) {
        this.address = address;
        this.port = port;
        this.password = password;
    }
    
    public RFBConnection(String address, String password) {
        this.address = address;
        this.port = DEFAULT_PORT;
        this.password = password;
    }

    public RFBConnection(String address) {
        this.address = address;
        this.port = DEFAULT_PORT;
        this.password = null;
    }

    public RFBConnection(InetAddress address, int port, String password) {
        this.inetAddress = address;
        this.port = port;
        this.password = password;
    }
    
    public RFBConnection(InetAddress address, String password) {
        this.inetAddress = address;
        this.port = DEFAULT_PORT;
        this.password = password;
    }

    public RFBConnection(InetAddress address) {
        this.inetAddress = address;
        this.port = DEFAULT_PORT;
        this.password = null;
    }
    
    public void setArd35Compatibility(boolean ard35Compatibility) {
        this.ard35Compatibility = ard35Compatibility;
    }
    public boolean getArd35Compatibility() {
        return ard35Compatibility;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public Version getServerVersion() {
        return serverVersion;
    }
    
    public void connect() throws UnknownHostException, IOException, RFBException {
        
        if ((address != null) && (address.equals(MAGIC_DEMO_HOSTNAME))) {
            this.serverVersion = new Version(MAX_VERSION);
            this.serverName = "demo";
            this.width = 1024;
            this.height = 768;
            this.pointerX = width/2.0f;
            this.pointerY = height/2.0f;
            return;
        }
        
        //System.out.println("connecting...");
        if (inetAddress != null) {
            socket = new Socket(inetAddress, port);
        } else {
            socket = new Socket(address, port);
        }
        socket.setSoTimeout(TIMEOUT);
        // disable Nagle's algorithm so mouse movements are smooth.
        socket.setTcpNoDelay(true);
        //System.out.println("connected.");

        this.stream = new RFBStream(socket.getInputStream(), socket.getOutputStream());
        //System.out.println("reading from the RFB socket...");
        
        // version

        this.serverVersion = stream.readVersion();
        int version = this.serverVersion.asInt();
        if (this.serverVersion.isAppleRemoteDesktop()) {
            // Apple Remote Desktop breaks VNC clients by reporting
            // a bogus version number -- v3.889 -- when it actually
            // uses something closer to v3.7.
            //System.out.println("Apple Remote Desktop detected -- falling back to RFB 3.7.");
            version = 0x0307; // use v3.7 for ARD
        } else {
            if (version < MIN_VERSION) {
                throw new RFBException("cannot support RFB version "+(new Version(version)));
            }
            if (version > MAX_VERSION) {
                version = MAX_VERSION;
            }
        }
        stream.writeVersion(version);
        
        // security
        
        byte[] securityTypes = stream.readSecurity();
        if (securityTypes == null) {
            throw new RFBException("error from server: "+stream.readString());
        }
        byte clientSecurityType = 0;
        for (byte securityType : securityTypes) {
            if (securityType == SECURITY_NONE) {
                // this type is preferred.
                clientSecurityType = SECURITY_NONE;
                break;
            } else if (securityType == SECURITY_VNCAUTH) {
                clientSecurityType = SECURITY_VNCAUTH;
            }
        }
        if (clientSecurityType == 0) {
            throw new RFBException("failure to negotiate security type - 1.");
        }
        stream.writeSecurity(clientSecurityType);
        
        // VNC Authentication (DES challenge-response)
        if (clientSecurityType == SECURITY_VNCAUTH) {
            if (password == null) {
                throw new RFBException("the server needs a password.");
            }
            stream.performVncAuthentication(password);
        }
        
        // read a security result... it's always sent in version 3.8.
        // previous versions skipped the result for SECURITY_NONE.
        if ((version >= 0x0308) || (clientSecurityType != SECURITY_NONE)) {
            // read a security result.
            if (stream.readSecurityResult() == 1) {
                // failure
                if (version >= 0x0308) {
                    throw new RFBException("error from server: "+stream.readString());
                } else {
                    if (clientSecurityType == SECURITY_VNCAUTH) {
                        throw new RFBException("cannot authenticate.  bad password?");
                    } else {
                        throw new RFBException("cannot authenticate with server.");
                    }
                }
            }
        }
        
        // initialization
        Object[] oa = stream.performInitialization();
        this.serverName = (String)oa[0];
        this.width = (Integer)oa[1];
        this.height = (Integer)oa[2];
        this.pointerX = width/2.0f;
        this.pointerY = height/2.0f;
    }
    
    public void disconnect() throws IOException {
        if (socket != null) {
            socket.shutdownOutput();
            socket.close();
            socket = null;
        }
    }
    
    // event handling
    
    /**
     * Dispatch incoming events
     */
    public void sendEvent(RFBEvent event) throws IOException {
        if (stream == null) {
            return;
        }
        if (event instanceof RFBKeyEvent) {
            sendKey((RFBKeyEvent)event);
        } else if (event instanceof RFBPointerEvent) {
            handlePointerEvent((RFBPointerEvent)event);
        }
    }
    
    private void sendKey(RFBKeyEvent keyEvent) throws IOException {
        stream.sendKey(keyEvent);
    }

    private float syAccumulator = 0.0f;
    private void handlePointerEvent(RFBPointerEvent rpe) throws IOException {
        float distance=0.0f, speed=0.0f;
        
        // handle movement
        if (rpe.dt > 0 && (rpe.dx != 0.0f || rpe.dy != 0.0f)) {
            // calculate speed
            distance = FloatMath.sqrt(rpe.dx*rpe.dx+rpe.dy*rpe.dy);
            speed = distance/((float)rpe.dt) * 10.0f;
            if (speed > 15.0f) {
                speed = 15.0f;
            } else if (speed < 0.1f) {
                speed = 0.1f;
            }
            
            // TODO TODO TODO
            // The following is an experiment in alternative speed-scaling formulas...
            /*
            float dxp = rpe.dx*speed;
            float dyp = rpe.dy*speed;
            float log = (float) Math.log(speed+1);
            float dxp2 = rpe.dx * log;
            float dyp2 = rpe.dy * log;
            float ex = (float) (speed+1)*(speed+1);
            float dxp3 = rpe.dx * ex;
            float dyp3 = rpe.dy * ex;
            System.out.printf(
                "RFBf: dx=%f dy=%f dt=%d speed=%f dx'=%f dy'=%f | log=%f dx''=%f dy''=%f | ex=%f dxe=%f dye=%f\n",
                rpe.dx, rpe.dy, rpe.dt, speed,
                dxp, dyp,
                log, dxp2, dyp2,
                ex, dxp3, dyp3
            );
            pointerX += dxp3/3;
            pointerY += dyp3/3;
            */

            pointerX += (rpe.dx*speed);
            pointerY += (rpe.dy*speed);
            
            if (pointerX >= width) {
                pointerX = width-1;
            }
            if (pointerY >= height) {
                pointerY = height-1;
            }
            if (pointerX < 0) {
                pointerX = 0;
            }
            if (pointerY < 0) {
                pointerY = 0;
            }
        }
        
        // handle buttons
        byte buttons = 0x00;
        if (rpe.button1) {
            buttons |= 0x01;
        }
        if (rpe.button2) {
            if (ard35Compatibility) {
                // After the July 2011 update for Apple Remote Desktop v3.5,
                // the ARD VNC server started using button2 for right-click,
                // instead of the standard button3.
                buttons |= 0x02;
            } else {
                buttons |= 0x04;
            }
        }
        
        // process scroll events
        // TODO: support horizontal scrolling
        int yScroll = 0;
        if (rpe.sy != 0.0f) {
            // scale the sy down
            float effectiveSy = rpe.sy/5;
            // accumulate the sy.  This allows us to track
            // fractional changes, but only send an event when
            // the absolute accumulation is greater than 1.0.
            syAccumulator += effectiveSy;
float sya1 = syAccumulator;
            if (Math.abs(syAccumulator) >= 1.0) {
                yScroll = (int)syAccumulator;
                // keep the fractional part
                syAccumulator = syAccumulator - yScroll;
                
                // clamp the yScroll
                if (yScroll < -20) {
                    yScroll = -20;
                } else if (yScroll > 20) {
                    yScroll = 20;
                }
            }
//System.out.printf("RFB>> rpe.sy=%f effectiveSy=%f syA1=%f yScroll=%d syA2=%f\n", rpe.sy, effectiveSy, sya1, yScroll, syAccumulator);
        } else {
            syAccumulator = 0.0f;
        }

        // send the event

        if (yScroll != 0) {
            // handle scroll
            if (yScroll > 0) {
                buttons |= 0x08;
            } else if (yScroll < 0) {
                buttons |= 0x10;
            }
            
            byte clearScrollButtons = buttons;
            clearScrollButtons &= (~(0x08 | 0x10));
            /*
            System.out.printf(
                "RFB>> dt=%d dx=%f dy=%f sx=%f sy=%f || dist=%f speed=%f >> %02X %f,%f  yScroll=%d\n",
                rpe.dt, rpe.dx, rpe.dy, rpe.sx, rpe.sy,
                distance, speed,
                buttons, pointerX, pointerY,
                yScroll
            );
            */
            stream.sendMultiplePointerEvents(Math.abs(yScroll), buttons, clearScrollButtons, (int)pointerX, (int)pointerY);
        } else {
            /*
            System.out.printf(
                "RFB>> dt=%d dx=%f dy=%f sx=%f sy=%f || dist=%f speed=%f >> %02X %f,%f\n",
                rpe.dt, rpe.dx, rpe.dy, rpe.sx, rpe.sy,
                distance, speed,
                buttons, pointerX, pointerY
            );
            */
            stream.sendPointerEvent(buttons, (int)pointerX, (int)pointerY);
        }
    }
}
