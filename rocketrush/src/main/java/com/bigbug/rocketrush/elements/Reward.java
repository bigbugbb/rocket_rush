package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Canvas;

import com.bigbug.rocketrush.basic.AppCtrl;
import com.bigbug.rocketrush.basic.AppObject;

import java.util.Random;

public class Reward extends AppObject {
		
	protected Random mRand = new Random();
	protected int mCanvasWidth  = 0;
	protected int mCanvasHeight = 0;
	protected boolean mBound = false;

	protected long mBoundTimeout   = 0;
	protected long mUnboundTimeout = 0; 
	protected long mBegTime = System.currentTimeMillis();

    protected Rocket mRocket;
	
	public Reward(Context context) {
		super(context);
		setMovable(true);				
		setTimeout(20000, 18000);
		// set speed for unbound state
		setSpeed((3 + mRand.nextInt(3)) * mDip, (3 + mRand.nextInt(2)) * mDip);
	}
	
	public void setTimeout(long boundTimeout, long unboundTimeout) {
		mBoundTimeout   = boundTimeout;
		mUnboundTimeout = unboundTimeout;
	}
	
	public void bindRocket(Rocket rocket) {
		setCollidable(false);
		rocket.bindReward(this);	
		mBound   = true;
		mRocket  = rocket;
		mBegTime = System.currentTimeMillis(); // update this time
		onBound();
	}
	
	public void unbindRocket() {
		if (mRocket == null) { // hasn't been bound
			return;
		}
		mRocket.unbindReward(this);
	}
	
	public boolean isTimeout() {
		return (System.currentTimeMillis() - mBegTime) > (mBound ? mBoundTimeout : mUnboundTimeout);
	}

	@Override
	public void onDraw(Canvas c) {
		if (mBound) {			
			drawBound(c);
		} else {
			drawUnbound(c);
		}
	}	

	@Override
	public void onUpdate() {
		if (mBound) {
			updateBound();
			if (isTimeout()) {
				unbindRocket();
			}
		} else {
			updateUnbound();			
		}
	}
	
	@Override
	public void release() {
		unbindRocket();
	}
	
	@Override
	public void operate(AppCtrl ctrl) {
	}

	@Override
	public void onSizeChanged(int width, int height) {
		mCanvasWidth  = width;
		mCanvasHeight = height;		
	}
	
	protected void onBound() {}
	protected void updateBound() {}
	protected void updateUnbound() {}
	protected void drawBound(Canvas c) {}
	protected void drawUnbound(Canvas c) {}
}
