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

package com.cafbit.valence.power;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.os.PowerManager;
import android.util.Log;

/**
 * A wrapper of the PowerManager that allows access to the hidden APIs.
 * 
 * NOTE: Put all access to hidden and regular PowerManager APIs in here to
 * have a central class for managing those APIs.
 */
public class PowerManagerWrapper {
    private static final String LOG_TAG = PowerManagerWrapper.class.getName();
    public static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = getPROXIMITY_SCREEN_OFF_WAKE_LOCK();
    public static final int WAIT_FOR_PROXIMITY_NEGATIVE = getWAIT_FOR_PROXIMITY_NEGATIVE();

    private final PowerManager mPowerManager;

    public PowerManagerWrapper(PowerManager powerManager) {
        mPowerManager = powerManager;
    }

    public WakeLockWrapper newWakeLock(int levelAndFlags, String tag) {
        return new DelegatingWakeLockWrapper(mPowerManager.newWakeLock(levelAndFlags, tag));
    }

    public boolean isWakeLockLevelSupported(int levelAndFlags) {
        try {
            Method isWakeLockLevelSupported = mPowerManager.getClass().getDeclaredMethod("isWakeLockLevelSupported", int.class);
            return (Boolean) isWakeLockLevelSupported.invoke(mPowerManager, levelAndFlags);
        } catch (Exception e) {
            Log.w(LOG_TAG, "reflection failed with error: ", e);
            return false;
        }
    }

    private static int getPROXIMITY_SCREEN_OFF_WAKE_LOCK() {
        // This is the value in the android.os.PowerManager.java source file for PROXIMITY_SCREEN_OFF_WAKE_LOCK
        // http://androidxref.com/4.2_r1/xref/frameworks/base/core/java/android/os/PowerManager.java#189
        return getPowerManagerInt("PROXIMITY_SCREEN_OFF_WAKE_LOCK", 0x00000020);
    }

    private static int getWAIT_FOR_PROXIMITY_NEGATIVE() {
        // This is the value in the android.os.PowerManager.java source file for WAIT_FOR_PROXIMITY_NEGATIVE
        // http://androidxref.com/4.2_r1/xref/frameworks/base/core/java/android/os/PowerManager.java#230
        return getPowerManagerInt("WAIT_FOR_PROXIMITY_NEGATIVE", 1);
    }

    private static int getPowerManagerInt(String fieldName, int defaultValue) {
        try {
            Field powerManagerStaticField = PowerManager.class.getDeclaredField(fieldName);

            // pass in "null" since powerManagerStaticField
            return powerManagerStaticField.getInt(null);
        } catch (Exception e) {
            Log.w(LOG_TAG, "reflection failed with error: ", e);
            return defaultValue;
        }
    }

}
