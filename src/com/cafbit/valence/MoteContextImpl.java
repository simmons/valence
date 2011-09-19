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

package com.cafbit.valence;

import java.util.Arrays;
import java.util.List;

import com.cafbit.motelib.MoteContext;
import com.cafbit.motelib.model.DeviceClass;
import com.cafbit.valence.device.ValenceDeviceClass;

import android.content.Context;

public class MoteContextImpl extends MoteContext {

    public static List<DeviceClass> deviceClasses =
        Arrays.asList(new DeviceClass[] {
            new ValenceDeviceClass()
        });

    public MoteContextImpl(Context context) {
        super(context);
    }

    @Override
    public String getDeviceWord(DeviceClass deviceClass, boolean plural, boolean capitalized) {
        if (plural) {
            return "VNC servers";
        } else {
            return "VNC server";
        }
    }

    @Override
    public List<DeviceClass> getAllDeviceClasses() {
        return deviceClasses;
    }

    @Override
    public int getSettingsXmlResource() {
        return R.xml.settings;
    }

}
