// Generic GameView class
// Aug 04 2010
// Johnny Watson

package com.vigasotech.brickening;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.graphics.Canvas;

public abstract class GameView extends SurfaceView {
	Context ctx;
	RenderThread renderThread;
	boolean hold = false;
	
	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
	}
	
	public GameView(Context context) {
		super(context);
		ctx = context;
	}
	
	public void initialize() {
		renderThread = new RenderThread(getHolder());
		renderThread.start();
	}
	
	public void pauseGame() {
		renderThread.suspend();
		hold = true;
	}
	
	public void resumeGame() {
		if(!hold) return;
		hold = false;
		// renderThread = new RenderThread(getHolder());
		renderThread.resume();
	}
	
	public abstract void updateGame(long elapsed);
	public abstract void drawGame(Canvas canvas, long elapsed);
    public abstract boolean onPrepareOptionsMenu(Menu menu);
    public abstract boolean onOptionsItemSelected(MenuItem item);
    public abstract void saveGameState(SharedPreferences data);
    public abstract void loadSaveData(SharedPreferences data);
	
	class RenderThread extends Thread implements SurfaceHolder.Callback {
		SurfaceHolder mSurfaceHolder;
		
		public RenderThread(SurfaceHolder holder) {
			super();
			mSurfaceHolder = holder;
		}
		
		long lastTime = 0;
		
		@Override
		public void run() {
			while(true) {
				long curTime = System.currentTimeMillis();
				updateGame(lastTime != 0 ? curTime - lastTime : 16);
				
				if(mSurfaceHolder.isCreating()) return;
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas();
					drawGame(c, lastTime != 0 ? curTime - lastTime : 16);
				} catch(Exception e) {
					Log.println(1, e.toString(), "");
				} finally {
					if(c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
						lastTime = curTime;
					}
				}
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
		}

		public void surfaceCreated(SurfaceHolder holder) {
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			suspend();
		}
	}
}
