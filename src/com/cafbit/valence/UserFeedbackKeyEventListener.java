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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.view.KeyEvent;
import android.widget.TextView;

import com.cafbit.valence.config.PreferencesConstants;

public class UserFeedbackKeyEventListener implements KeyEventListener {

    private final TextView textview;
    private final TimeoutString text;
    private final UpdateTextView updateTextView;
    private final SharedPreferences preferences;

    public UserFeedbackKeyEventListener(TextView view, SharedPreferences preferences) {
        this(view, preferences, new TimeoutString(preferences));
    }

    public UserFeedbackKeyEventListener(TextView view,
            SharedPreferences pref,
            TimeoutString t) {

        textview = view;
        preferences = pref;
        text = t;
        updateTextView = new UpdateTextView(textview, text, preferences);

        updateText();
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent) {

        if (!preferences.getBoolean(PreferencesConstants.SHOW_USER_TEXT_OVERLAY, true)) {
            return;
        }

        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
            return;
        }

        boolean textUpdated = false;

        // handle the special case of a ACTION_MULTIPLE event with key code of KEYCODE_UNKNOWN,
        // which signals a raw string of characters associated with the event.
        if (keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE
                && keyEvent.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN
                && keyEvent.getCharacters() != null) {

            text.append(keyEvent.getCharacters());
            textUpdated = true;
        } else if (keyEvent.getUnicodeChar() != 0) {
            text.appendCodePoint(keyEvent.getUnicodeChar());
            textUpdated = true;
        } else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DEL) {
            text.removeLastCharacter();
            textUpdated = true;
        }

        // TODO: handle special text characters like non-printable characters eg. meta characters

        // TODO: do something different with characters when meta characters are pressed. 

        if (textUpdated) {
            updateText();
        }
    }

    private void updateText() {
        // use View.post(Runnable) to ensure that the update gets performed on 
        // the UI thread.
        textview.post(updateTextView);
    }

    private static class UpdateTextView implements Runnable, OnSharedPreferenceChangeListener {

        /**
         * The delay between running this task repeatedly. Since there is no
         * user viewable counter for how long it took to actually remove the
         * user text from the UI, we can run the repeated tasks less often.
         */
        private static final long RESHEDULE_DELAY_MILLISECONDS = 500;

        private final TextView textview;
        private final TimeoutString timeoutString;
        private final SharedPreferences preferences;

        public UpdateTextView(TextView view,
                TimeoutString timeoutString,
                SharedPreferences pref) {

            textview = view;
            this.timeoutString = timeoutString;
            preferences = pref;

            preferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void run() {
            boolean reschedule = false;

            String text = timeoutString.getString();
            if (text == null || text.isEmpty()) {
                // TODO: move this preference check and default text into the 
                // render method of a TextView subclass so that the default text 
                // always gets rendered correctly after a preference change.
                if (preferences.getBoolean(PreferencesConstants.SHOW_TOUCHPAD_HINT_OVERLAY, true)) {
                    textview.setText(R.string.touchpad_hint_overlay);
                } else {
                    textview.setText("");
                }
                reschedule = false;
            } else if (text.equals(textview.getText())) {
                reschedule = true;
            } else {
                textview.setText(text);
                reschedule = true;
            }

            if (reschedule) {
                doSchedule(RESHEDULE_DELAY_MILLISECONDS);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
            // really we could check to see if we need to reschedule, but we 
            // keep the code simpler by running the update code again and 
            // keeping the rescheduling logic all in one place.   
            doSchedule(0);
        }

        private void doSchedule(long delay) {
            // If there was two key events, remove all secondary instances 
            // of this job in the handler queue to not have this job be 
            // queued up more times than is necessary.
            textview.removeCallbacks(this);

            // re-schedule this task so that eventually the timeout will be
            // reached for the text and user text will be replaced with the
            // hint text. 
            textview.postDelayed(this, delay);
        }
    }

    private static class TimeoutString implements OnSharedPreferenceChangeListener {
        private static final String DEFAULT_TIMEOUT_SECONDS = "15";

        private long deadline_milliseconds = 0;
        private StringBuffer stringBuffer = new StringBuffer();
        private final SharedPreferences preferences;

        public TimeoutString(SharedPreferences pref) {
            preferences = pref;

            preferences.registerOnSharedPreferenceChangeListener(this);
        }

        public synchronized void append(CharSequence s) {
            stringBuffer.append(s);
            updateDeadline();
        }

        public synchronized void appendCodePoint(int codePoint) {
            stringBuffer.appendCodePoint(codePoint);
            updateDeadline();
        }

        public synchronized void removeLastCharacter() {
            if (stringBuffer.length() > 0) {
                stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                updateDeadline();
            }
        }

        public synchronized String getString() {
            if (System.currentTimeMillis() > deadline_milliseconds) {
                stringBuffer.setLength(0); // clear the string buffer
                return "";
            } else {
                return stringBuffer.toString();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
            // really we could check to see if we need to reschedule, but we 
            // keep the code simpler by running the update code again and 
            // keeping the rescheduling logic all in one place.   
            updateDeadline();
        }

        private void updateDeadline() {
            deadline_milliseconds = System.currentTimeMillis()
                    + getUserPreferenceTimeout();
        }

        private long getUserPreferenceTimeout() {
            return Long.valueOf(preferences.getString(
                    PreferencesConstants.USER_TEXT_OVERLAY_TIMEOUT_SECONDS,
                    DEFAULT_TIMEOUT_SECONDS)) * 1000;
        }
    }
}
