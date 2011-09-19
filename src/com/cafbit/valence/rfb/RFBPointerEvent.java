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

/**
 *
 */
package com.cafbit.valence.rfb;

public class RFBPointerEvent implements RFBEvent {
    public long dt = 0L;
    public float dx = 0.0f, dy = 0.0f;
    public boolean button1 = false;
    public boolean button2 = false;
    public float sx = 0.0f;
    public float sy = 0.0f;
}