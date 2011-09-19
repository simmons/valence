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

import java.net.Socket;

import com.cafbit.valence.RFBThread.RFBThreadHandler;

public class RFBRecvThread extends Thread {

    private static int serial = 0;

    private RFBThreadHandler parentHandler;
    private Socket socket;
    private boolean valid = true;

    public RFBRecvThread(RFBThreadHandler handler, Socket socket) {
        this.parentHandler = handler;
        this.socket = socket;
        setName("rfbrecv-"+(serial++));
    }

    public void invalidate() {
        valid = false;
    }

    @Override
    public void run() {
        if (socket == null) {
            // demo mode
            return;
        }
        byte buffer[] = new byte[4096];
        while (true) {
            int ret;
            try {
                socket.setSoTimeout(0);
                ret = socket.getInputStream().read(buffer);
            } catch (Exception e) {
                if (valid) {
                    parentHandler.error(e);
                }
                break;
            }
            if (ret == -1) {
                if (valid) {
                    parentHandler.onRecvDisconnect();
                }
                break;
            }
        }
    }

}
