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

package com.cafbit.valence;

import java.io.IOException;
import java.net.InetAddress;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.cafbit.valence.ValenceActivity.ValenceHandler;
import com.cafbit.valence.rfb.RFBConnection;
import com.cafbit.valence.rfb.RFBEvent;
import com.cafbit.valence.rfb.RFBSecurity;

public class RFBThread extends Thread {

    private static final long DETACH_TIMEOUT = 10*1000; // 10 seconds
    private static final long ACTIVITY_TIMEOUT = 10*60*1000; // 10 minutes

    private static int serial = 0;

    private ValenceHandler parentHandler;
    private RFBThreadHandler myHandler;
    private RFBConnection conn;
    private RFBRecvThread recvThread;
    private boolean connected = false;

    public RFBThread(ValenceHandler handler, String address, int port, RFBSecurity security) {
        this.parentHandler = handler;
        this.conn = new RFBConnection(address, port, security);
        setName("rfb-"+(serial++));
    }

    public RFBThread(ValenceHandler handler, String address, RFBSecurity security) {
        this.parentHandler = handler;
        this.conn = new RFBConnection(address, security);
        setName("rfb-"+(serial++));
    }

    public void setArd35Compatibility(boolean ard35Compatibility) {
        this.conn.setArd35Compatibility(ard35Compatibility);
    }
    public boolean getArd35Compatibility() {
        return this.conn.getArd35Compatibility();
    }

    public void setValenceHandler(ValenceHandler valenceHandler) {
        this.parentHandler = valenceHandler;
    }

    public RFBThreadHandler getHandler() {
        return myHandler;
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void run() {
        // set up the IPC
        Looper.prepare();
        this.myHandler = new RFBThreadHandler();

        // connect to the RFB server
        try {
            conn.connect();
        } catch (Exception e) {
            e.printStackTrace();
            parentHandler.error(e);
            return;
        }

        // set up the receiving thread
        // (we don't use received data.  this just gobbles bytes, on
        // the off chance that the server sends us something.)
        recvThread = new RFBRecvThread(myHandler, conn.getSocket());
        recvThread.start();

        // notify the parent that we are connected
        parentHandler.onConnect();
        connected = true;

        // loop!
        Looper.loop();

        connected = false;
        recvThread.invalidate();
        recvThread = null;
        try {
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class RFBThreadHandler extends Handler {

        public static final int MSG_QUIT = 1;
        public static final int MSG_ERROR = 2;
        public static final int MSG_RECV_DISCONNECT = 3;
        public static final int MSG_RFB_EVENT = 4;
        public static final int MSG_TIMEOUT = 5;

        public RFBThreadHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //System.out.println("RFB thread message "+msg.what);

            switch (msg.what) {
            case MSG_TIMEOUT:
                Log.w("Valence", "Closing RFB connection due to timeout.");
            case MSG_QUIT:
                if (recvThread != null) {
                    recvThread.invalidate();
                }
                Looper.myLooper().quit();
                Log.w("Valence", "RFB thread shutting down.");
                break;
            case MSG_ERROR:
                if (recvThread != null) {
                    recvThread.invalidate();
                }
                parentHandler.error((Throwable)msg.obj);
                Looper.myLooper().quit();
                break;
            case MSG_RECV_DISCONNECT:
                if (recvThread != null) {
                    recvThread.invalidate();
                }
                parentHandler.onDisconnect();
                Looper.myLooper().quit();
                break;
            case MSG_RFB_EVENT:
                RFBEvent event = (RFBEvent)msg.obj;
                try {
                    conn.sendEvent(event);
                } catch (IOException e) {
                    if (recvThread != null) {
                        recvThread.invalidate();
                    }
                    parentHandler.error(e);
                    Looper.myLooper().quit();
                }
                break;
            }

        }

        // helper methods

        public void quit() {
            sendMessage(Message.obtain(this, MSG_QUIT));
        }

        public void error(Throwable throwable) {
            sendMessage(Message.obtain(this, MSG_ERROR, throwable));
        }

        public void onRecvDisconnect() {
            sendMessage(Message.obtain(this, MSG_RECV_DISCONNECT));
        }

        public void onRFBEvent(RFBEvent event) {
            sendMessage(Message.obtain(this, MSG_RFB_EVENT, event));
        }

        public void onDetach() {
            sendMessageDelayed(Message.obtain(this, MSG_TIMEOUT), DETACH_TIMEOUT);
        }

        public void onReattach() {
            removeMessages(MSG_TIMEOUT);
        }

        public void onActivityPause() {
            sendMessageDelayed(Message.obtain(this, MSG_TIMEOUT), ACTIVITY_TIMEOUT);
        }

        public void onActivityResume() {
            removeMessages(MSG_TIMEOUT);
        }
}

}
