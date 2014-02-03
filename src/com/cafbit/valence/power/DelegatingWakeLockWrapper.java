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

import java.lang.reflect.Method;

import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * A WakeLockWrapper that delegates to a real WakeLock and exposes the
 * hidden APIs.
 * 
 */
public class DelegatingWakeLockWrapper implements WakeLockWrapper {
    private static final String LOG_TAG = "Valence.DelegatingWakeLockWrapper";
    private final WakeLock mWakeLock;

    public DelegatingWakeLockWrapper(WakeLock wakeLock) {
        mWakeLock = wakeLock;
    }

    @Override
    public void acquire(long timeout) {
        mWakeLock.acquire(timeout);
    }

    @Override
    public void release() {
        mWakeLock.release();
    }

    /**
     * Releases the wake lock with flags to modify the release behavior.
     * 
     * See: http://androidxref.com/4.4
     * .2_r1/xref/frameworks/base/core/java/android/os/PowerManager.java#739
     */
    @Override
    public void release(int flags) {
        try {
            Method release = mWakeLock.getClass().getDeclaredMethod("release", Integer.class);
            release.invoke(mWakeLock, flags);
        } catch (Exception e) {
            Log.w(LOG_TAG, "reflection failed with error: ", e);
            mWakeLock.release();
        }
    }

    @Override
    public boolean isHeld() {
        return mWakeLock.isHeld();
    }
}
