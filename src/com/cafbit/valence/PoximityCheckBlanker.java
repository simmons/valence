/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2014 Alexandre Quesnel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cafbit.valence;

import android.app.Activity;
import android.util.Log;
import android.view.View;

public class PoximityCheckBlanker {

    /**
     * The time to wait before enabling the blank the screen due to the
     * proximity sensor.
     */
    private static final long PROXIMITY_BLANK_DELAY_MILLIS = 100;
    /**
     * The time to wait before disabling the blank the screen due to the
     * proximity sensor.
     */
    private static final long PROXIMITY_UNBLANK_DELAY_MILLIS = 500;

    private ProximitySensorManager mProximitySensorManager;
    private final ProximitySensorListener mProximitySensorListener = new ProximitySensorListener();

    private Activity activity;
    private View blankView;

    public PoximityCheckBlanker(Activity activity) {
        this.activity = activity;
        mProximitySensorManager = new ProximitySensorManager(activity, mProximitySensorListener);
//        blankView = new View(activity);
//        blankView.setBackgroundColor(Color.BLACK);
//        blankView.setVisibility(View.GONE);
//        blankView.setClickable(true);
    }

    /** Listener to changes in the proximity sensor state. */
    private class ProximitySensorListener implements ProximitySensorManager.Listener {
        /** Used to show a blank view and hide the action bar. */
        private final Runnable mBlankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = activity.findViewById(R.id.blank);
                blankView.setVisibility(View.VISIBLE);
//                activity.getActionBar().hide();
                Log.d("Valence", "Blanking the screen");
            }
        };
        /** Used to remove the blank view and show the action bar. */
        private final Runnable mUnblankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = activity.findViewById(R.id.blank);
                blankView.setVisibility(View.GONE);
//                activity.getActionBar().show();
                Log.d("Valence", "Unblanking the screen");
            }
        };

        @Override
        public synchronized void onNear() {
            clearPendingRequests();
            postDelayed(mBlankRunnable, PROXIMITY_BLANK_DELAY_MILLIS);
        }

        @Override
        public synchronized void onFar() {
            clearPendingRequests();
            postDelayed(mUnblankRunnable, PROXIMITY_UNBLANK_DELAY_MILLIS);
        }

        /** Removed any delayed requests that may be pending. */
        public synchronized void clearPendingRequests() {
            View blankView = activity.findViewById(R.id.blank);
            blankView.removeCallbacks(mBlankRunnable);
            blankView.removeCallbacks(mUnblankRunnable);
        }

        /** Post a {@link Runnable} with a delay on the main thread. */
        private synchronized void postDelayed(Runnable runnable, long delayMillis) {
            // Post these instead of executing immediately so that:
            // - They are guaranteed to be executed on the main thread.
            // - If the sensor values changes rapidly for some time, the UI will not be
            //   updated immediately.
            View blankView = activity.findViewById(R.id.blank);
            blankView.postDelayed(runnable, delayMillis);
//            runnable.run();
        }
    }

    public void enableProximitySensor() {
        mProximitySensorManager.enable();
    }

    public void disableProximitySensor(boolean waitForFarState) {
        mProximitySensorManager.disable(waitForFarState);
    }
}
