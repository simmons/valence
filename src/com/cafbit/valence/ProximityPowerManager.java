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

import android.content.SharedPreferences;

import com.cafbit.valence.config.PreferencesConstants;
import com.cafbit.valence.power.WakeLockFactory;
import com.cafbit.valence.power.WakeLockWrapper;

/**
 * A class that turns off the screen (to save battery power) based on the
 * proximity sensor.
 * 
 */
public class ProximityPowerManager {
    private static final String LOG_TAG = "Valence.ProximityPowerManager";
    private static final int DEFAULT_PROXIMITY_TIMEOUT_MILLISECONDS = 1 * 60 * 60 * 1000;

    private final WakeLockWrapper mProximityWakeLock;
    private final SharedPreferences mPreferences;

    public ProximityPowerManager(WakeLockFactory wakeLockFactory, SharedPreferences preferences) {

        mPreferences = preferences;
        mProximityWakeLock = wakeLockFactory.getInstance(LOG_TAG);
    }

    /**
     * Aquire the wake lock to:
     * - Keep the device awake.
     * - Turn off the screen when the proximity sensor detects and object close
     * to the screen.
     * 
     * Note: the wake lock will be held for a user defined maximum amount of
     * time before the wake lock is released and the device is allowed to go to
     * sleep.
     * 
     * 
     */
    public void aquire() {
        synchronized (mProximityWakeLock) {
            // Note: aquireing the wake lock needs to be responsive to the user 
            // changing the preference setting without needing to restart the app.
            if (isProximityPowerManagementEnabled()
                    && !mProximityWakeLock.isHeld()) {
                mProximityWakeLock.acquire(
                        mPreferences.getInt(
                                PreferencesConstants.PROXIMITY_TIMEOUT_MILLISECONDS,
                                DEFAULT_PROXIMITY_TIMEOUT_MILLISECONDS));
            }
        }
    }

    /**
     * Release the wake lock and allow the device to go to sleep immediately.
     */
    public void release() {
        synchronized (mProximityWakeLock) {
            if (mProximityWakeLock.isHeld()) {
                mProximityWakeLock.release();
            }
        }
    }

    private boolean isProximityPowerManagementEnabled() {
        return mPreferences.getBoolean(PreferencesConstants.SCREEN_OFF_ON_PROXIMITY_SENSOR, true);
    }
}
