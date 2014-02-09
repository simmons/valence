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

package com.cafbit.valence.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import android.content.SharedPreferences;

public class NullSharedPreferences implements SharedPreferences {

    @Override
    public boolean contains(String preferenceName) {
        return false;
    }

    @Override
    public Editor edit() {
        return null;
    }

    @Override
    public Map<String, ?> getAll() {
        return Collections.emptyMap();
    }

    @Override
    public boolean getBoolean(String preferenceName, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public float getFloat(String preferenceName, float defaultValue) {
        return defaultValue;
    }

    @Override
    public int getInt(String preferenceName, int defaultValue) {
        return defaultValue;
    }

    @Override
    public long getLong(String preferenceName, long defaultValue) {
        return defaultValue;
    }

    @Override
    public String getString(String preferenceName, String defaultValue) {
        return defaultValue;
    }

    @Override
    public Set<String> getStringSet(String preferenceName, Set<String> defaultValue) {
        return defaultValue;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener arg0) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener arg0) {
    }

}
