/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/
 *
 * Copyright 2014 Alexandre Quesnel
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

public class RFBKeyEvent implements RFBEvent {

    public enum Action {
        KEY_UP,
        KEY_DOWN,
        KEY_DOWN_AND_UP;

        public boolean isKeyUp() {
            return KEY_UP.equals(this) || KEY_DOWN_AND_UP.equals(this);
        }

        public boolean isKeyDown() {
            return KEY_DOWN.equals(this) || KEY_DOWN_AND_UP.equals(this);
        }

        public static Action fromKeyEvent(KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                return KEY_DOWN;
            } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                return KEY_UP;
            } else {
                throw new IllegalArgumentException("Unknown KeyEvent action. KeyEvent = " + keyEvent.toString());
            }
        }
    }

    private final int keysym;
    private final Action action;

    public RFBKeyEvent(KeyEvent keyEvent) {
        this(KeyTranslator.translate(keyEvent), Action.fromKeyEvent(keyEvent));
    }

    public RFBKeyEvent(char ch) {
        this(KeyTranslator.translate(ch), Action.KEY_DOWN_AND_UP);
    }

    private RFBKeyEvent(int keysym, Action action) {
        this.keysym = keysym;
        this.action = action;
    }

    public int getKeySym() {
        return keysym;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public String toString() {

        return "RFBKeyEvent("
                + "keysym=[0x"
                + Integer.toHexString(keysym)
                + "], action=["
                + action.toString()
                + "]";

    }
}
