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
    }

    public static class SpecialKey {
        public String name;
        public String shortName;
        public int keysym;

        public static SpecialKey SHIFT = new SpecialKey("Shift", "shift", 0xFFE1);
        public static SpecialKey CTRL = new SpecialKey("Ctrl", "ctrl", 0xFFE3);
        public static SpecialKey ALT = new SpecialKey("Alt", "alt", 0xFFE9);
        public static SpecialKey WIN = new SpecialKey("Win/Cmd", "win", 0xFFEB);

        public SpecialKey(String name, int keysym) {
            this.name = name;
            this.shortName = name;
            this.keysym = keysym;
        }

        public SpecialKey(String name, String shortName, int keysym) {
            this.name = name;
            this.shortName = shortName;
            this.keysym = keysym;
        }

        @Override
        public String toString() {

            return "SpecialKey("
                    + "name=["
                    + (name == null ? "null" : name.toString())
                    + "], shortName=["
                    + (shortName == null ? "null" : shortName.toString())
                    + "], keysym=["
                    + Integer.toHexString(keysym)
                    + "]";

        }
    };

    public static final SpecialKey MODIFIERS[] = {
            SpecialKey.SHIFT,
            SpecialKey.CTRL,
            SpecialKey.ALT,
            SpecialKey.WIN
    };

    public static final SpecialKey SPECIALS[] = {
            new SpecialKey("Esc", 0xFF1B),
            new SpecialKey("Tab", 0xFF09),
            new SpecialKey("F1", 0xFFBE),
            new SpecialKey("F2", 0xFFBF),
            new SpecialKey("F3", 0xFFC0),
            new SpecialKey("F4", 0xFFC1),
            new SpecialKey("F5", 0xFFC2),
            new SpecialKey("F6", 0xFFC3),
            new SpecialKey("F7", 0xFFC4),
            new SpecialKey("F8", 0xFFC5),
            new SpecialKey("F9", 0xFFC6),
            new SpecialKey("F10", 0xFFC7)
    };

    private final int keysym;
    private final Action action;

    public RFBKeyEvent(KeyEvent keyEvent) {
        this(KeyTranslator.translate(keyEvent), Action.KEY_DOWN_AND_UP);
    }

    public RFBKeyEvent(SpecialKey special) {
        this(special.keysym, Action.KEY_DOWN_AND_UP);
    }

    public RFBKeyEvent(SpecialKey special, Action action) {
        this(special.keysym, action);
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
