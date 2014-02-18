/*
 * Copyright (C) 2014 Alexandre Quesnel
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

import android.view.KeyEvent;

import com.cafbit.valence.KeyEventListener;
import com.cafbit.valence.RFBThread.RFBThreadHandler;
import com.cafbit.valence.RFBThread.RFBThreadHandlerReadyCallback;

public class RFBKeyEventListener implements KeyEventListener, RFBThreadHandlerReadyCallback {

    private RFBThreadHandler rfbHandler = null;

    @Override
    public void onKeyEvent(KeyEvent keyEvent) {
        if (shouldIgnoreKeyCode(keyEvent.getKeyCode())) {
            return;
        }

        // handle the special case of a ACTION_MULTIPLE event with key code of KEYCODE_UNKNOWN,
        // which signals a raw string of characters associated with the event.
        if (keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE
                && keyEvent.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {

            String chars = keyEvent.getCharacters();
            if (chars != null) {
                for (char c : chars.toCharArray()) {
                    sendKey(new RFBKeyEvent(c));
                }
            }
        } else {
            sendKey(new RFBKeyEvent(keyEvent));
        }
    }

    @Override
    public void onHandlerReady(RFBThreadHandler handler) {
        rfbHandler = handler;
    }

    private void sendKey(RFBKeyEvent rfbKeyEvent) {
        if (rfbHandler != null) {
            rfbHandler.onRFBEvent(rfbKeyEvent);
        }
    }

    private boolean shouldIgnoreKeyCode(int keyCode) {
        return (keyCode == KeyEvent.KEYCODE_MENU) || (keyCode == KeyEvent.KEYCODE_BACK);
    }
}
