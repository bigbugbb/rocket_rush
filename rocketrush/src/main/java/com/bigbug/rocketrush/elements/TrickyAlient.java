package com.bigbug.rocketrush.elements;

import android.content.Context;

import com.bigbug.rocketrush.Constants;


public class TrickyAlient extends Alient {

	private int mType = 0;
	
	public TrickyAlient(Context context, int type) {
		super(context);
		mType = type;
	}
	
	@Override
	public void initSpeeds(float x, float y, int accTime) {		
		float accSpeedX = - x / ((mType == 0 ? 2000 : 320) / Constants.DATA_UPDATE_INTERVAL);
		float accSpeedY = y / (1000 / Constants.DATA_UPDATE_INTERVAL);
		setSpeed(x, y + accSpeedY * accTime);
		setMinSpeed(0, y);
		setMaxSpeed(x, y + y);
		setAccSpeed(accSpeedX, accSpeedY);
	}

	@Override
	public void onUpdate() {
		if (mSpeedUnchangeable) {
			mX += mSpeedX;
			mY += mSpeedY;
			return;
		}
		
		if (mAccMoveDuration > 0) {
			mSpeedY = Math.min(mSpeedY + mAccSpeedY, mMaxSpeedY);
			mAccMoveDuration -= Constants.DATA_UPDATE_INTERVAL;
		} else {
			mSpeedY = Math.max(mSpeedY - mAccSpeedY, mMinSpeedY);
		}
		
		if (mType == 0) {
			mSpeedX += mAccSpeedX;			
		} else if (mType == 1) {
			if (Math.abs(Math.abs(mSpeedX) - mMaxSpeedX) <= 1e-2) {
				if (mSpeedX > 0) { // right
					mAccSpeedX = -Math.abs(mAccSpeedX);
				} else {
					mAccSpeedX = Math.abs(mAccSpeedX);
				}
			}
			mSpeedX += mAccSpeedX;			
		}

		mX += mSpeedX;
		mY += mSpeedY;		
	}

	@Override
	public void triggerCollideEffect(int kind, float x, float y) {
		super.triggerCollideEffect(kind, x, y);		
	}
}
