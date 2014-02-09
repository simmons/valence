/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/
 * 
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.cafbit.valence.config.NullSharedPreferences;
import com.cafbit.valence.config.PreferencesConstants;

public class TouchPadView extends View {

    private TouchPadHandler touchPadHandler;
    private DisplayMetrics metrics = new DisplayMetrics();
    private final SharedPreferences preferences;

    public static interface OnTouchPadEventListener {
        public void onTouchPadEvent(TouchPadEvent event);
    };

    public TouchPadView(Context context) {
        this(context, null);
    }

    public TouchPadView(Context context, AttributeSet attrs) {
        super(context, attrs);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (pref == null) {
            preferences = new NullSharedPreferences();
        } else {
            preferences = pref;
        }

        this.setBackgroundColor(Color.BLACK);
        drawSetup();

        if (this.isInEditMode()) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float xdpi, ydpi;
            xdpi = metrics.xdpi;
            ydpi = metrics.ydpi;
            touchPadHandler = new TouchPadHandler(this, xdpi, ydpi);
        } else {
            throw new RuntimeException("need activity reference for metrics");
        }
    }

    public void setOnTouchPadEvent(OnTouchPadEventListener onTouchPadEvent) {
        touchPadHandler.setOnTouchPadEventListener(onTouchPadEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touchPadHandler.onTouchEvent(event);
    }

    // ///////////////////////////////////////////////////////////////

    private Paint linePaint = new Paint();
    private Paint grayLinePaint = new Paint();
    private Paint fontPaint = new Paint();
    // private Path cornerPath;
    final int s = 4; // spacing
    final int l = 20; // length

    private void drawSetup() {
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(2.0f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        grayLinePaint.setColor(Color.DKGRAY);
        grayLinePaint.setStrokeWidth(2.0f);
        grayLinePaint.setStyle(Paint.Style.STROKE);
        grayLinePaint.setAntiAlias(true);

        fontPaint.setTextSize(24.0f);
        fontPaint.setColor(Color.WHITE);
        // fontPaint.setStrokeWidth(2.0f);
        fontPaint.setStyle(Paint.Style.STROKE);
        fontPaint.setAntiAlias(true);

        /*
         * // define the corner path cornerPath = new Path();
         * cornerPath.moveTo(l+s, 0); cornerPath.lineTo(0, 0);
         * cornerPath.lineTo(0, l+s); cornerPath.moveTo(l+s, s);
         * cornerPath.lineTo(s, s); cornerPath.lineTo(s, l+s);
         * cornerPath.moveTo(s, s); cornerPath.lineTo(l+s, l+s);
         */
    }

    private static class TextBox {
        public int w = 0;
        public int h = 0;
    }

    private TextBox getTextBox(Paint paint, String[] lines) {
        Rect bounds = new Rect();
        TextBox box = new TextBox();
        float fontSpacing = fontPaint.getFontSpacing();
        for (String line : lines) {
            fontPaint.getTextBounds(line, 0, line.length(), bounds);
            int w = bounds.right - bounds.left;
            if (w > box.w) {
                box.w = w;
            }
            box.h += fontSpacing;
        }
        return box;
    }

    private void renderText(Canvas canvas, float yposPercent, String[] lines) {
        Rect bounds = new Rect();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int targetWidth = (int) (viewWidth * 0.75);
        int maxHeight = (int) (viewHeight * 0.75);
        float ts = 25.0f;
        final int MAX_TEXT_SIZE = 32;
        TextBox box;

        // calculate the optimal text size
        int i = 0;
        while (true) {
            fontPaint.setTextSize(ts);
            box = getTextBox(fontPaint, lines);

            float adjustment;
            boolean isYadjustment = false;
            if (box.h > maxHeight) {
                adjustment = maxHeight / (float) box.h;
                isYadjustment = true;
            } else {
                adjustment = targetWidth / (float) box.w;
            }
            if (((i >= 9) && (!isYadjustment)) || (i >= 12) || (adjustment > 0.99f && adjustment < 1.05f)) {
                break;
            } else {
                ts = ts * adjustment;
            }
            i++;
        }
        if (ts > MAX_TEXT_SIZE) {
            ts = MAX_TEXT_SIZE;
            fontPaint.setTextSize(ts);
        }
        box = getTextBox(fontPaint, lines);
        // System.out.printf("canvas: %dx%d  textbox: %dx%d  size: %f\n",
        // viewWidth, viewHeight, textWidth, textHeight, ts);

        // draw the text
        float ypos = viewHeight * yposPercent - (box.h / 2.0f) + ts;
        if (ypos < ts) {
            ypos = ts;
        }
        for (String line : lines) {
            float tw = fontPaint.measureText(line);
            canvas.drawText(line, (viewWidth - tw) / 2, ypos, fontPaint);
            ypos += fontPaint.getFontSpacing();
        }

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        // System.out.println("----- onLayout:"+changed+", "+left+", "+top+", "+right+", "+bottom);
        invalidate();
        // TODO Auto-generated method stub
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        int w = this.getWidth();
        int h = this.getHeight();

        float boxPoints[] = new float[] {
                0, 0, w, 0,
                w, 0, w, h,
                w, h, 0, h,
                0, h, 0, 0
        };
        float xPoints[] = new float[] {
                // "X"
                2, 2, w - 2, h - 2,
                2, h - 2, w - 2, 2,
                // side marks
                4, h / 2 - 15, 4, h / 2 + 15,
                4, h / 2, 4 + 30, h / 2,
                w - 4, h / 2 - 15, w - 4, h / 2 + 15,
                w - 4, h / 2, w - 4 - 30, h / 2,
                w / 2 - 15, 4, w / 2 + 15, 4,
                w / 2, 4, w / 2, 4 + 30,
                w / 2 - 15, h - 4, w / 2 + 15, h - 4,
                w / 2, h - 4, w / 2, h - 4 - 30,
        };
        canvas.drawLines(boxPoints, linePaint);
        canvas.drawLines(xPoints, grayLinePaint);

        if (preferences.getBoolean(PreferencesConstants.SHOW_TOUCHPAD_HINT_OVERLAY, true)) {
            renderText(canvas, 0.5f, new String[] {
                    "Use this area as a touchpad.",
                    "Tap to click.",
                    "Tap then swipe to drag.",
                    "Two-finger tap to right click.",
                    "Two-finger swipe to scroll."
            });
        }

        return;
    }

    // ///////////////////////////////////////////////////////////////
    // Input management
    // ///////////////////////////////////////////////////////////////

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    /**
     * The HTC soft keyboard requires us to manage the keyboard input, and
     * backspace over discarded characters if the user performed a long-press to
     * select an alternative character.
     */
    public static class ValenceInputConnection extends BaseInputConnection {
        public ValenceInputConnection(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            boolean valid = true;
            KeyEvent deleteEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
            if ((rightLength == 0) && (leftLength == 0)) {
                valid = this.sendKeyEvent(deleteEvent);
            } else {
                for (int i = 0; i < leftLength; i++) {
                    valid = this.sendKeyEvent(deleteEvent);
                    if (!valid) {
                        break;
                    }
                }
            }
            return valid;
        }
    }

    /**
     * The HTC soft keyboard requires us to manage the keyboard input, and
     * backspace over discarded characters if the user performed a long-press to
     * select an alternative character.
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        outAttrs.imeOptions |= EditorInfo.IME_ACTION_NONE;
        outAttrs.inputType = EditorInfo.TYPE_NULL;
        return new ValenceInputConnection(this, false);
    }
}
