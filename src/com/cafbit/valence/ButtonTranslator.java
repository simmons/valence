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

package com.cafbit.valence;

import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.View;

import com.cafbit.valence.hacks.HackKeyCode;

public class ButtonTranslator {

    private static final SparseIntArray BUTTON_TO_KEY_MAPPING = initializeMapping();

    private static SparseIntArray initializeMapping() {
        SparseIntArray mapping = new SparseIntArray();

        mapping.put(R.id.ButtonF1, KeyEvent.KEYCODE_F1);
        mapping.put(R.id.ButtonF2, KeyEvent.KEYCODE_F1);
        mapping.put(R.id.ButtonF3, KeyEvent.KEYCODE_F3);
        mapping.put(R.id.ButtonF4, KeyEvent.KEYCODE_F4);
        mapping.put(R.id.ButtonF5, KeyEvent.KEYCODE_F5);
        mapping.put(R.id.ButtonF6, KeyEvent.KEYCODE_F6);
        mapping.put(R.id.ButtonF7, KeyEvent.KEYCODE_F7);
        mapping.put(R.id.ButtonF8, KeyEvent.KEYCODE_F8);
        mapping.put(R.id.ButtonF9, KeyEvent.KEYCODE_F9);
        mapping.put(R.id.ButtonF10, KeyEvent.KEYCODE_F10);
        mapping.put(R.id.ButtonF11, KeyEvent.KEYCODE_F11);
        mapping.put(R.id.ButtonF12, KeyEvent.KEYCODE_F12);

        mapping.put(R.id.ButtonEsc, KeyEvent.KEYCODE_ESCAPE);
        mapping.put(R.id.ButtonTab, KeyEvent.KEYCODE_TAB);
        mapping.put(R.id.ButtonHome, KeyEvent.KEYCODE_HOME);
        mapping.put(R.id.ButtonEnd, HackKeyCode.KEYCODE_END);
        mapping.put(R.id.ButtonPrintScreen, HackKeyCode.KEYCODE_PRINT_SCREEN);
        mapping.put(R.id.ButtonPageUp, KeyEvent.KEYCODE_PAGE_UP);
        mapping.put(R.id.ButtonPageDown, KeyEvent.KEYCODE_PAGE_DOWN);
        mapping.put(R.id.ButtonDelete, KeyEvent.KEYCODE_FORWARD_DEL);
        mapping.put(R.id.ButtonBackspace, KeyEvent.KEYCODE_DEL);
        mapping.put(R.id.ButtonInsert, KeyEvent.KEYCODE_INSERT);
        mapping.put(R.id.ButtonUp, KeyEvent.KEYCODE_DPAD_UP);
        mapping.put(R.id.ButtonDown, KeyEvent.KEYCODE_DPAD_DOWN);
        mapping.put(R.id.ButtonLeft, KeyEvent.KEYCODE_DPAD_LEFT);
        mapping.put(R.id.ButtonRight, KeyEvent.KEYCODE_DPAD_RIGHT);

        mapping.put(R.id.ButtonShift, KeyEvent.KEYCODE_SHIFT_LEFT);
        mapping.put(R.id.ButtonCtrl, KeyEvent.KEYCODE_CTRL_LEFT);
        mapping.put(R.id.ButtonAlt, KeyEvent.KEYCODE_ALT_LEFT);
        mapping.put(R.id.ButtonWin, KeyEvent.KEYCODE_WINDOW);

        return mapping;
    }

    public static int toKeyCode(View view) {
        return BUTTON_TO_KEY_MAPPING.get(view.getId());
    }
}
