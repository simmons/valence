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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.view.KeyEvent;

public class RFBStream {

    private static final int BUFFER_SIZE = 4096;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private InputStream inputStream;
    private OutputStream outputStream;
    private int version;

    private enum State {
        NEED_VERSION
    };
    private State state = State.NEED_VERSION;

    public RFBStream(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
        this.outputStream = outputStream;
    }

    public RFBMessage read() throws IOException, RFBException {
        return null;
    }

    byte[] read(int length) throws IOException {

        byte[] buffer = new byte[length];
        int pos = 0;

        while (pos < length) {
            //System.out.println("reading.  pos="+pos+" length="+length);
            int nbytes = inputStream.read(buffer, pos, length-pos);
            if (nbytes != -1) {
                pos+=nbytes;
            }
        }
        //System.out.println("read:\n"+Util.hexDump(buffer));

        return buffer;
    }

    int readByte() throws IOException {
        byte[] ba = read(1);
        return ba[0];
    }
    int readShort() throws IOException {
        byte[] ba = read(2);
        return (0xFF & (int)ba[0])<<8 | (0xFF & (int)ba[1])<<0;
    }
    int readInt() throws IOException {
        byte[] ba = read(4);
        return ba[0]<<24 | ba[1]<<16 | ba[2]<<8 | ba[3];
    }
    String readString() throws IOException {
        int length = readInt();
        byte[] buffer = read(length);
        return new String(buffer, "ASCII");
    }

    void write(byte[] ba) throws IOException {
        //System.out.println("write:\n"+Util.hexDump(ba));
        outputStream.write(ba);
    }
    void writeByte(int b) throws IOException {
        //byte[] ba = new byte[1]; ba[0] = (byte)b; System.out.println("write:\n"+Util.hexDump(ba));
        outputStream.write(b);
    }
    void write(RFBMessage message) throws IOException {
        //System.out.println("write:\n"+Util.hexDump(message.getBytes()));
        outputStream.write(message.getBytes());
    }

    public Version readVersion() throws IOException, RFBException {
        byte[] buffer;

        buffer = read(12);
        Version version = new Version(buffer);
        return version;
    }

    public void writeVersion(int version) throws IOException {
        this.version = version;
        Version v = new Version(version);
        write(v);
    }

    public byte[] readSecurity() throws IOException {
        byte[] securityTypes;

        if (version >= 0x0307) {
            int num = readByte();
            if (num == 0) {
                return null;
            }
            securityTypes = read(num);
        } else {
            securityTypes = new byte[1];
            securityTypes[0] = (byte)readInt();
            if (securityTypes[0] == RFBConnection.SECURITY_INVALID) {
                return null;
            }
        }
        return securityTypes;
    }

    public void writeSecurity(byte securityType) throws IOException {
        writeByte(securityType);
    }

    public int readSecurityResult() throws IOException {
        return readInt();
    }

    public Object[] performInitialization() throws IOException {
        // send ClientInit: shared-flag = 1;
        writeByte(0x01);

        // read the ServerInit
        byte[] buf = read(20);
        String name = readString();

        // parse the ServerInit
        int width = (buf[0]&0xFF)<<8 | (int)(buf[1] & 0xFF);
        int height = (buf[2]&0xFF)<<8 | (int)(buf[3] & 0xFF);

        //System.out.println("server-init: \""+name+"\" "+width+"x"+height);
        return new Object[] { name, width, height };

        // TODO: remove this testing code
        // TESTING:
        /*
        byte[] keyDown = new byte[] {
            0x04, // message-type
            0x01, // down-flag
            0x00, 0x00, // padding
            0x00, 0x00, 0x00, 0x41 // keysym
        };
        byte[] keyUp = new byte[] {
            0x04, // message-type
            0x00, // down-flag
            0x00, 0x00, // padding
            0x00, 0x00, 0x00, 0x41 // keysym
        };

        write(keyDown);
        write(keyUp);
        write(keyDown);
        write(keyUp);

        int x = 5;
        int y = 5;
        for (int i=0; i<15; i++) {
            byte[] pointerEvent = new byte[] {
                0x05, // message-type
                0x7F, // button-mask
                (byte)((x&0xFF00)>>8), (byte)(x&0xFF),
                (byte)((y&0xFF00)>>8), (byte)(y&0xFF),
            };
            write(pointerEvent);
            pointerEvent[1] = 0x00;
            write(pointerEvent);
            x+=2;
            y+=2;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        */
    }

    public void sendPointerEvent(byte buttons, int x, int y) throws IOException {
        byte[] pointerEvent = new byte[] {
            0x05, // message-type
            buttons, // button-mask
            (byte)((x&0xFF00)>>8), (byte)(x&0xFF),
            (byte)((y&0xFF00)>>8), (byte)(y&0xFF),
        };
        write(pointerEvent);
    }

    /**
     * Pack multiple pointer events into a single packet for performance.
     * (Since we've disabled Nagle's algorithm, we have to think about
     * these things.)
     */
    public void sendMultiplePointerEvents(int iterations, byte setButtons, byte clearButtons, int x, int y) throws IOException {
        byte[] buffer = new byte[6*iterations*2];
        byte x1 = (byte)((x&0xFF00)>>8);
        byte x2 = (byte)(x&0xFF);
        byte y1 = (byte)((y&0xFF00)>>8);
        byte y2 = (byte)(y&0xFF);
        for (int i=0; i<iterations; i++) {
            int n = 6*i*2;
            buffer[n]   = 0x05;         // message-type
            buffer[n+1] = setButtons;   // button-mask
            buffer[n+2] = x1;
            buffer[n+3] = x2;
            buffer[n+4] = y1;
            buffer[n+5] = y2;
            buffer[n+6] = 0x05;         // message-type
            buffer[n+7] = clearButtons; // button-mask
            buffer[n+8] = x1;
            buffer[n+9] = x2;
            buffer[n+10] = y1;
            buffer[n+11] = y2;
        }
        write(buffer);
    }

    public void sendKey(RFBKeyEvent keyEvent) throws IOException {
        // resolve the keysym
        int keysym;
        if (keyEvent.special != null) {
            keysym = keyEvent.special.keysym;
        } else if (keyEvent.ch != 0) {
            keysym = KeyTranslator.translate(keyEvent.ch);
        } else {
            keysym = KeyTranslator.translate(keyEvent.keyEvent);
            if (keysym == 0) {
                return;
            }
        }
        if (keysym == 0) {
            return;
        }

        if (keyEvent.modifier != null) {
            sendKeyDown(keyEvent.modifier.keysym);
        }

        sendKeyDown(keysym);
        sendKeyUp(keysym);

        if (keyEvent.modifier != null) {
            sendKeyUp(keyEvent.modifier.keysym);
        }
    }

    private void sendKeyDown(int keysym) throws IOException {
        byte[] keyDown = new byte[] {
            0x04, // message-type
            0x01, // down-flag
            0x00, 0x00, // padding
            (byte) ((keysym >> 24) & 0xFF),
            (byte) ((keysym >> 16) & 0xFF),
            (byte) ((keysym >> 8) & 0xFF),
            (byte) ((keysym) & 0xFF)
        };
        write(keyDown);
    }

    private void sendKeyUp(int keysym) throws IOException {
        byte[] keyUp = new byte[] {
            0x04, // message-type
            0x00, // down-flag
            0x00, 0x00, // padding
            (byte) ((keysym >> 24) & 0xFF),
            (byte) ((keysym >> 16) & 0xFF),
            (byte) ((keysym >> 8) & 0xFF),
            (byte) ((keysym) & 0xFF)
        };
        write(keyUp);
    }

}
