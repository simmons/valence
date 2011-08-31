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

import android.view.KeyEvent;

public class RFBKeyEvent implements RFBEvent {
    
    public static class SpecialKey {
        public String name;
        public String shortName;
        public int keysym;
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
    };
    
    public static final SpecialKey MODIFIERS[] = {
        new SpecialKey("Shift", "shift", 0xFFE1),
        new SpecialKey("Ctrl", "ctrl", 0xFFE3),
        new SpecialKey("Alt", "alt", 0xFFE9),
        new SpecialKey("Win/Cmd", "win", 0xFFEB)
    };
    
    public static final SpecialKey SPECIALS[] = {
        new SpecialKey("Esc", 0xFF1B),
        new SpecialKey("Tab", 0xFF09),
        new SpecialKey("F1",  0xFFBE),
        new SpecialKey("F2",  0xFFBF),
        new SpecialKey("F3",  0xFFC0),
        new SpecialKey("F4",  0xFFC1),
        new SpecialKey("F5",  0xFFC2),
        new SpecialKey("F6",  0xFFC3),
        new SpecialKey("F7",  0xFFC4),
        new SpecialKey("F8",  0xFFC5),
        new SpecialKey("F9",  0xFFC6),
        new SpecialKey("F10", 0xFFC7)
    };
    

    // the android key event will be stored in one of the following three formats:
    KeyEvent keyEvent;
    char ch = 0;
    SpecialKey special = null;
    
    SpecialKey modifier = null;

    public RFBKeyEvent(KeyEvent keyEvent) {
        this.keyEvent = keyEvent;
    }

    public RFBKeyEvent(KeyEvent keyEvent, SpecialKey modifier) {
        this.keyEvent = keyEvent;
        this.modifier = modifier;
    }

    public RFBKeyEvent(SpecialKey special, SpecialKey modifier) {
        this.special = special;
        this.modifier = modifier;
    }

    public RFBKeyEvent(char ch, SpecialKey modifier) {
        this.ch = ch;
        this.modifier = modifier;
    }

}
