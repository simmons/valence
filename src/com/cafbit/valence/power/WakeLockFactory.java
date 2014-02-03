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

import android.os.PowerManager;

public class WakeLockFactory {

    private final PowerManagerWrapper mPowerManagerWrapper;

    public WakeLockFactory(PowerManager powerManager) {
        mPowerManagerWrapper = new PowerManagerWrapper(powerManager);
    }

    public WakeLockWrapper getInstance(String logTag) {

        if (mPowerManagerWrapper.isWakeLockLevelSupported(
                PowerManagerWrapper.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {

            return mPowerManagerWrapper.newWakeLock(
                    PowerManagerWrapper.PROXIMITY_SCREEN_OFF_WAKE_LOCK, logTag);
        } else {
            return new NullWakeLockWrapper();
        }
    }
}
