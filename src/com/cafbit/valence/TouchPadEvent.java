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
package com.cafbit.valence;

import com.cafbit.valence.TouchPadHandler.Touch;

public class TouchPadEvent {
    long dt = 0L;
    float dx = 0.0f, dy = 0.0f;
    boolean button1 = false;
    boolean button2 = false;
    float sx = 0.0f; // scroll x
    float sy = 0.0f; // scroll y

    public static TouchPadEvent[] tap(boolean multiTouchTap) {
        TouchPadEvent events[] = new TouchPadEvent[2];
        events[0] = new TouchPadEvent();
        if (multiTouchTap) {
            events[0].button2 = true;
        } else {
            events[0].button1 = true;
        }
        events[1] = new TouchPadEvent();
        return events;
    }

    public static TouchPadEvent move(Touch touch) {
        TouchPadEvent event = new TouchPadEvent();
        event.dt = touch.dt;
        event.dx = touch.dx;
        event.dy = touch.dy;
        return event;
    }

    public static TouchPadEvent[] startDrag(Touch touch) {
        TouchPadEvent events[] = new TouchPadEvent[2];
        events[0] = new TouchPadEvent();
        events[0].button1 = true;
        events[1] = new TouchPadEvent();
        events[1].dt = touch.dt;
        events[1].dx = touch.dx;
        events[1].dy = touch.dy;
        events[1].button1 = true;
        return events;
    }

    public static TouchPadEvent drag(Touch touch) {
        TouchPadEvent event = new TouchPadEvent();
        event.dt = touch.dt;
        event.dx = touch.dx;
        event.dy = touch.dy;
        event.button1 = true;
        return event;
    }

    public static TouchPadEvent scroll(Touch touch) {
        TouchPadEvent event = new TouchPadEvent();
        event.sx = touch.sx;
        event.sy = touch.sy;
        return event;
    }

    public static TouchPadEvent clear() {
        return new TouchPadEvent();
    }

    public void debug() {
        System.out.printf("TouchPadEvent dt=%d dx=%f dy=%f b1=%b b2=%b vs=%f hs=%f\n",
            dt, dx, dy,
            button1, button2,
            sy, sx
        );
    }
}