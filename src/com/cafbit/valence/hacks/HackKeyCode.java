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

package com.cafbit.valence.hacks;

import android.view.KeyEvent;

/**
 * This class contains reinterpretations of key codes that are defined by the
 * Android KeyEvent class into keycodes that are not defined by that class.
 * Since we are using the KeyEvent class to hold the key codes that we
 * are using this class defines how we reinterpret those key codes that should
 * be meaningless for a VNC session on a desktop.
 */
public class HackKeyCode {
    /**
     * HACK: there is no KeyEvent.KEYCODE_END constant, so we choose one that
     * should not come up for a VNC session.
     */
    public static final int KEYCODE_END = KeyEvent.KEYCODE_ENDCALL;

    /**
     * HACK: there is no KeyEvent.KEYCODE_PRINT_SCREEN constant, so we choose
     * one that should not come up for a VNC session.
     */
    public static final int KEYCODE_PRINT_SCREEN = KeyEvent.KEYCODE_EXPLORER;
}
