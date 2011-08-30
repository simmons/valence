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

import com.cafbit.valence.TouchPadView.OnTouchPadEventListener;

import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

public class TouchPadHandler {
	
	private static final int TAP_TIME_THRESHOLD = 250; // 250ms
	private static final float TAP_DISTANCE_THRESHOLD = 6.0f;
	
	private float xdpi;
	private float ydpi;
	private long lastTapTime = 0L;
	private View view;

	/**
	 * A "touch" represents a complete series of motion events from
	 * down to up.  This class tracks the state of such touches and
	 * performs some simple dt/dx/dy calculations for the most recent
	 * motion event.
	 * 
	 * Note that a useful gesture may require multiple touches --
	 * for example, a "drag" requires two touches: a tap, then a
	 * swipe.  Therefore, some small amount of state must be maintained
	 * outside of this Touch object.
	 */
	public class Touch {
		// current state
		public long t;
		public float x, y;
		public long dt;
		public float dx, dy, d;
		public int pc;
		public boolean drag;
		// aggregate state
		public float totalDistance = 0.0f;
		public boolean multiTouch = false; // any multi-touch in history?

		public void clear() {
			t = 0;
			x = 0.0f;
			y = 0.0f;
			dt = 0;
			dx = 0.0f;
			dy = 0.0f;
			d = 0.0f;
			pc = 0;
			drag = false;
			totalDistance = 0.0f;
			multiTouch = false;
		}

		// down
		public void down(NormalMotionEvent nme) {
			t = nme.t;
			x = nme.x * 100.0f / xdpi;
			y = nme.y * 100.0f / ydpi;
			dt = 0;
			dx = 0.0f;
			dy = 0.0f;
			d = 0.0f;
			pc = nme.pc;
			drag = false;
			totalDistance = 0.0f;
			if (pc > 1) {
				multiTouch = true;
			}
		}
		
		public void move(NormalMotionEvent nme) {
			// normalize absolutes against the dpi
			long nt = nme.t;
			float nx = nme.x * 100.0f / xdpi;
			float ny = nme.y * 100.0f / ydpi;
			
			// calculate deltas - unless this is a transition
			// from multi-pointer to 1-pointer
			if (nme.pc == 1 && pc > 1) {
				// transition to 1-pointer - zero the deltas
				dt = 0;
				dx = 0.0f;
				dy = 0.0f;
			} else {
				dt = nt - t;
				dx = nx - x;
				dy = ny - y;
			}
			
			// update absolutes
			t = nt;
			x = nx;
			y = ny;
			
			// update pointer count
			pc = nme.pc;
			
			// update aggregates
			d = FloatMath.sqrt(dx*dx+dy*dy);
			totalDistance += d;
			
			if (pc > 1) {
				multiTouch = true;
				handleScroll(nme);
			} else {
				// reset scroll state
				currentDirection = 0;
				newDirection = 0;
				newDirectionDistance = 0.0f;
				sx = 0.0f;
				sy = 0.0f;
			}
		}
		
		private int currentDirection = 0;
		private int newDirection = 0;
		private float newDirectionDistance = 0.0f;
		public float sx = 0.0f;
		public float sy = 0.0f;
		private static final float CHANGE_DIRECTION_THRESHOLD = 3.0f;
		private void handleScroll(NormalMotionEvent nme) {
			// disregard insignificant movements
			if (Math.abs(touch.dx) < 0.1f && Math.abs(touch.dy) < 0.1f) {
				//System.out.println("scroll: abandon "+touch.dx+","+touch.dy);
				return;
			}
			
			int direction = 0; // UNKNOWN
			if (Math.abs(touch.dx) > Math.abs(touch.dy*3)) {
				if (touch.dx > 0) {
					direction = 1; // RIGHT
				} else {
					direction = 3; // LEFT
				}
			} else if (Math.abs(touch.dy) > Math.abs(touch.dx*3)) {
				if (touch.dy > 0) {
					direction = 2; // DOWN
				} else {
					direction = 4; // UP
				}
			}
			
			if ((currentDirection != 0) && (direction != currentDirection)) {
				if (direction == newDirection) {
					newDirectionDistance += d;
				} else {
					newDirectionDistance = d;
				}
				if (newDirectionDistance < CHANGE_DIRECTION_THRESHOLD) {
					newDirection = direction;
					sx = 0.0f;
					sy = 0.0f;
					//System.out.println("scroll: resist new direction");
					return;
				} else {
					//System.out.println("scroll: change direction");
					currentDirection = direction;
				}
			}
			
			switch (direction) {
			case 0:
				break;
			case 1:
				sx = +d;
				break;
			case 2:
				sy = -d;
				break;
			case 3:
				sx = -d;
				break;
			case 4:
				sy = +d;
				break;
			}
			
			currentDirection = direction;
		}
		
		public boolean isMoved() {
			if ((dx != 0.0f) || (dy != 0.0f)) {
				return true;
			} else {
				return false;
			}
		}
		
		public boolean isScrolled() {
			if ((sx != 0.0f) || (sy != 0.0f)) {
				return true;
			} else {
				return false;
			}
		}
		
		public void drag() {
			this.drag = true;
		}
	};
	private Touch touch = new Touch();
	
	/**
	 * A NormalMotionEvent is like a MotionEvent, except that historical
	 * event batches have been flattened out into individual units.
	 */
	private static class NormalMotionEvent {
		int action;
		long t;
		float x, y;
		int pc;
		
		static float lastX=0.0f, lastY=0.0f;
		
		private void init(MotionEvent me) {
			this.action = me.getAction();
			this.t = me.getEventTime();
			this.x = me.getX();
			this.y = me.getY();
			this.pc = me.getPointerCount();
			
			if (pc>1) {
				float smallestDistance = 999999.0f;
				int nearestPointer = -1;
				for (int i=0; i<pc; i++) {
					float px = me.getX(i);
					float py = me.getY(i);
					float dx = px-lastX;
					float dy = py-lastY;
					float d = FloatMath.sqrt(dx*dx+dy*dy);
					if (d < smallestDistance) {
						smallestDistance = d;
						nearestPointer = i;
					}
				}
				float px = me.getX(nearestPointer);
				float py = me.getY(nearestPointer);
				if ((px != this.x) || (py != this.y)){
					this.x = px;
					this.y = py;
				}
			}
			lastX = x;
			lastY = y;
		}

		private void init(MotionEvent me, int pos) {
			this.action = me.getAction();
			this.t = me.getHistoricalEventTime(pos);
			this.x = me.getHistoricalX(pos);
			this.y = me.getHistoricalY(pos);
			this.pc = me.getPointerCount();
			
			if (pc>1) {
				float lastDistance = 999999.0f;
				int nearestPointer = -1;
				for (int i=0; i<pc; i++) {
					float px = me.getHistoricalX(i, pos);
					float py = me.getHistoricalY(i, pos);
					float dx = px-lastX;
					float dy = py-lastY;
					float d = FloatMath.sqrt(dx*dx+dy*dy);
					if (d < lastDistance) {
						lastDistance = d;
						nearestPointer = i;
					}
				}
				float px = me.getHistoricalX(nearestPointer, pos);
				float py = me.getHistoricalY(nearestPointer, pos);
				if ((px != this.x) || (py != this.y)){
					this.x = px;
					this.y = py;
				}
			}
			lastX = x;
			lastY = y;
		}
		
		private static final int NUM_POOL_OBJECTS = 20;
		private static NormalMotionEvent pool[] = new NormalMotionEvent[NUM_POOL_OBJECTS];
		private static NormalMotionEvent array1[] = new NormalMotionEvent[1];
		private static NormalMotionEvent array2[] = new NormalMotionEvent[2];
		private static NormalMotionEvent array3[] = new NormalMotionEvent[3];
		private static NormalMotionEvent array4[] = new NormalMotionEvent[4];
		static {
			for (int i=0; i<NUM_POOL_OBJECTS; i++) {
				pool[i] = new NormalMotionEvent();
			}
		};
		
		public static NormalMotionEvent[] debatchMotionEvent(MotionEvent me) {
			int hs = me.getHistorySize();
			int j=0;
			for (int i=0; i<hs; i++) {
				// if this historical event has the same timestamp
				// as the previous one, combine it.
				long t = me.getHistoricalEventTime(i);
				if ((j > 0) && (pool[j-1].t == t)) {
					// keep your friends close, and your nme's closer.
					pool[j-1].x = me.getHistoricalX(i);
					pool[j-1].y = me.getHistoricalY(i);
				} else {
					pool[j++].init(me, i);
				}
				if (j == (NUM_POOL_OBJECTS-1)) {
					break;
				}
			}
			pool[j++].init(me);
			
			// premature optimization?
			switch (j) {
			case 1:
				array1[0] = pool[0];
				return array1;
			case 2:
				array2[0] = pool[0];
				array2[1] = pool[1];
				return array2;
			case 3:
				array3[0] = pool[0];
				array3[1] = pool[1];
				array3[2] = pool[2];
				return array3;
			case 4:
				array4[0] = pool[0];
				array4[1] = pool[1];
				array4[2] = pool[2];
				array4[3] = pool[3];
				return array4;
			default:
				NormalMotionEvent nmes[] = new NormalMotionEvent[j];
				System.arraycopy(pool, 0, nmes, 0, j);
				return nmes;
			}
		}

	};
	
	/**
	 * A PendingTap holds information about a tap that is scheduled
	 * to be delivered in the future, unless a subsequent event
	 * results in its cancellation.
	 */
	public class PendingTap implements Runnable {
		public boolean sent = false;
		private boolean canceled = false;
		private boolean multiTouchTap = false;
		
		public PendingTap(boolean multiTouchTap) {
			this.multiTouchTap = multiTouchTap;
		}
		
		public void cancel() {
			canceled = true;
		}

		@Override
		public void run() {
			if (canceled) {
				return;
			}
			send(TouchPadEvent.tap(multiTouchTap));
			sent = true;
		}
	};
	private PendingTap pendingTap = null;
	public void scheduleTap(NormalMotionEvent nme, boolean multiTouchTap) {
		if (pendingTap != null) {
			pendingTap.cancel();
		}
		pendingTap = new PendingTap(multiTouchTap);
		view.postDelayed(pendingTap, TAP_TIME_THRESHOLD);
	}
	public void cancelTap() {
		if (pendingTap != null) {
			pendingTap.cancel();
		}
		pendingTap = null;
	}

	////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////
	
	public TouchPadHandler(View view, float xdpi, float ydpi) {
		this.view = view;
		this.xdpi = xdpi;
		this.ydpi = ydpi;
	}
	
	private OnTouchPadEventListener onTouchPadEventListener = null;
	public void setOnTouchPadEventListener(OnTouchPadEventListener onTouchPadEventListener) {
		this.onTouchPadEventListener = onTouchPadEventListener;
	}
	
	private static final boolean debug = false;
	private static final String actions[] = {
		"ACTION_DOWN",				// 0
		"ACTION_UP",				// 1
		"ACTION_MOVE",				// 2
		"ACTION_CANCEL",			// 3
		"ACTION_OUTSIDE",			// 4
		"ACTION_POINTER_1_DOWN",	// 5
		"ACTION_POINTER_1_UP",		// 6
		"ACTION_HOVER_MOVE", 		// 7 - API 12
		"ACTION_SCROLL"				// 8 - API 12
	};
	private static final void debugEvent(MotionEvent me) {
		if (debug) {
			String action;
			int a = me.getAction();
			if (a < actions.length) {
				action = actions[a];
			} else {
				int pid = (a & 0xFF00) >> 8;
				switch (a & 0xFF) {
				case 5:
					action = "ACTION_POINTER_"+(pid+1)+"_DOWN";
					break;
				case 6:
					action = "ACTION_POINTER_"+(pid+1)+"_UP";
					break;
				default:
					action = "ACTION_UNKNOWN";
				}
			}
			
			System.out.printf(
				"%-22s t=%d x=%f y=%f pc=%d hs=%d down=%d\n",
				action,
				me.getEventTime(),
				me.getX(),
				me.getY(),
				me.getPointerCount(),
				me.getHistorySize(),
				me.getDownTime()
			);
			
			for (int i=0; i<me.getHistorySize(); i++) {
				System.out.printf(
					"%22s t=%d x=%f y=%f pc=%d\n",
					"["+i+"]",
					me.getHistoricalEventTime(i),
					me.getHistoricalX(i),
					me.getHistoricalY(i),
					me.getPointerCount()
				);
			}
		}
	};
	
	public void send(TouchPadEvent tpe) {
		if (onTouchPadEventListener != null) {
			onTouchPadEventListener.onTouchPadEvent(tpe);
		}
	}

	public void send(TouchPadEvent tpes[]) {
		if (onTouchPadEventListener != null) {
			for (TouchPadEvent tpe : tpes) {
				onTouchPadEventListener.onTouchPadEvent(tpe);
			}
		}
	}

	private final int STATE_INITIAL = 0;
	private final int STATE_DOWN = 1;
	private final int STATE_MULTI_DOWN = 2;
	private int state = STATE_INITIAL;
	
	public boolean onTouchEvent(MotionEvent me) {
		if (debug) {
			debugEvent(me);
		}
		for (NormalMotionEvent nme : NormalMotionEvent.debatchMotionEvent(me)) {
			switch (state) {
			case STATE_INITIAL:
				initial(nme);
				break;
			case STATE_DOWN:
				down(nme);
				break;
			case STATE_MULTI_DOWN:
				multiDown(nme);
				break;
			}
		}
		
		return true;
	}
	
	private void clearState() {
		touch.clear();
		state = STATE_INITIAL;
	}
	
	private void initial(NormalMotionEvent nme) {
		switch (nme.action) {
		case MotionEvent.ACTION_DOWN:
			state = STATE_DOWN;
			touch.down(nme);
			break;
		}
	}

	private void down(NormalMotionEvent nme) {
		
		// fix for the "samsung two-finger tap" bug
		if ( ((nme.action & MotionEvent.ACTION_POINTER_ID_MASK) > 0) &&
			 //((nme.action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) &&
			 (nme.pc > 1)
		) {
			touch.multiTouch = true;
		}
		
		// handle the multi-pointer down cases
		/*
		if ( ((nme.action & MotionEvent.ACTION_POINTER_ID_MASK) > 0) &&
			 ((nme.action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) &&
			 (nme.pc > 1)
		) {
			// just note the multi-touch
			touch.multiTouch = true;
			/ *
			//System.out.println("MULTI POINTER DOWN detected - transition")
			// multi-finger -- cancel any drag and
			// transition to STATE_MULTI_DOWN
			if (touch.drag) {
				// cancel the drag by releasing the mouse button
				send(TouchPadEvent.clear());				
			}
			state = STATE_MULTI_DOWN;
			// forward immediately to new state handler
			multiDown(nme);
			return;
			* /
		}
		*/
		
		// handle the other cases 
		switch (nme.action) {
		case MotionEvent.ACTION_DOWN:
			// throw away the existing DOWN state,
			// and start fresh.
			if (touch.drag) {
				// cancel the drag by releasing the mouse button
				send(TouchPadEvent.clear());				
			}
			clearState();
			state = STATE_DOWN;
			touch.down(nme);
			break;
		case MotionEvent.ACTION_UP:
			if (touch.totalDistance < TAP_DISTANCE_THRESHOLD) {
				if ((nme.t - lastTapTime) < TAP_TIME_THRESHOLD) {
					// a tap has been detected shortly after another tap,
					// so the user is obviously trying to click in rapid
					// succession and not initiate a drag.
					// kill any pending tap (and double-tap if necessary)
					if (pendingTap != null) {
						if (! pendingTap.canceled) {
							// go ahead and send this tap
							// before sending the current tap
							send(TouchPadEvent.tap(touch.multiTouch));
						}
						cancelTap();
					}
					
					// send this tap
					send(TouchPadEvent.tap(touch.multiTouch));
				} else {
					// schedule a tap to be sent in the future
					scheduleTap(nme, touch.multiTouch);
				}
				lastTapTime = nme.t;
			} else {
				if (touch.drag) {
					// stop the drag by releasing the mouse button
					send(TouchPadEvent.clear());
				}
			}
			clearState();
			break;
		case MotionEvent.ACTION_MOVE:
			if (nme.pc > 1) {
				// multi-finger -- cancel any drag and
				// transition to STATE_MULTI_DOWN
				if (touch.drag) {
					// cancel the drag by releasing the mouse button
					send(TouchPadEvent.clear());				
				}
				state = STATE_MULTI_DOWN;
				// forward immediately to new state handler
				multiDown(nme);
			}
			touch.move(nme);
			if (touch.drag) {
				// send the drag movement event
				if (touch.isMoved()) {
					send(TouchPadEvent.drag(touch));
				}
			} else {
				if ((nme.t - lastTapTime) < TAP_TIME_THRESHOLD) {
					// start dragging
					cancelTap();
					touch.drag();
					// initiate the drag
					send(TouchPadEvent.startDrag(touch));
				} else {
					// send a non-drag move event
					if (touch.isMoved()) {
						send(TouchPadEvent.move(touch));
					}
				}
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (touch.drag) {
				// cancel the drag by releasing the mouse button
				send(TouchPadEvent.clear());				
			}
			clearState();
			break;
		}
	}

	private void multiDown(NormalMotionEvent nme) {
		switch (nme.action) {
		case MotionEvent.ACTION_DOWN:
			// transition-forward to INITIAL
			clearState();
			initial(nme);
			break;
		case MotionEvent.ACTION_UP:
			// forward to STATE_DOWN ACTION_UP handler
			// to handle a two-finger tap
			state = STATE_DOWN;
			down(nme);
			break;
		case MotionEvent.ACTION_MOVE:
			if (nme.pc == 1) {
				// single-finger -- transition back
				// to STATE_DOWN.
				state = STATE_DOWN;
				down(nme);
			}
			touch.move(nme);
			if (touch.isScrolled()) {
				send(TouchPadEvent.scroll(touch));
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			clearState();
			break;
		}
	}

}
