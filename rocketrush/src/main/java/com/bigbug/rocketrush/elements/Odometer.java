package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import com.bigbug.rocketrush.Constants;
import com.bigbug.rocketrush.basic.AppCtrl;
import com.bigbug.rocketrush.basic.AppScale;

public class Odometer extends Utility {

	protected int mAccMoveDuration = 0;
	protected int mOdometer = 0;
	protected int mTarget = 100;
	protected int mMilestone = 5000;
	protected Paint mPaint = null;
	protected OnOdometerUpdateListener mOdometerUpdateListener = null; 
	public final static float DEFAULT_SPEED_X = 0;
	public final static float DEFAULT_SPEED_Y = 3f;
	
	public Odometer(Context context) {
		super(context);
		setKind(ODOMETER);
		setCollidable(false);
		setZOrder(ZOrders.ODOMETER);
		setSpeed(DEFAULT_SPEED_X, DEFAULT_SPEED_Y);
		setMaxSpeed(0, DEFAULT_SPEED_Y * 3);
		setAccSpeed(0, DEFAULT_SPEED_Y / (1000 / Constants.DATA_UPDATE_INTERVAL));
		
		mX = 24 * mDip;
        mY = 72 * mDip;
		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setARGB(255, 0, 0, 0);
		mPaint.setStyle(Style.FILL);	
		mPaint.setColor(Color.WHITE);
		mPaint.setTextSize(AppScale.doScaleT(30));
		mPaint.setFakeBoldText(true);
		mPaint.setShadowLayer(2 * mDip, mDip, mDip, Color.BLACK);
	}
	
	public int getDistance() {
		return mOdometer / 10;
	}
	
	public void setOdometerUpdateListener(OnOdometerUpdateListener listener) {
		mOdometerUpdateListener = listener;
	}
	
	@Override
	public void operate(AppCtrl ctrl) {
		int command = ctrl.getCommand();
		
		if (command == AppCtrl.MOVE_VERT) {
			mAccMoveDuration = 1000;
		} 
	}

	@Override
	public void onUpdate() {
		if (!mEnable) {
			return;
		}
		if (mAccMoveDuration > 0) {
			mSpeedY = Math.min(mSpeedY + mAccSpeedY, mMaxSpeedY);			
			mAccMoveDuration -= Constants.DATA_UPDATE_INTERVAL;
		} else {
			mSpeedY = Math.max(mSpeedY - mAccSpeedY, DEFAULT_SPEED_Y);
		}
		
		mOdometer += mSpeedY;
		if (mOdometer >= mTarget) {
			if (mOdometerUpdateListener != null) {
				mOdometerUpdateListener.onReachTarget(mOdometer);
				if (mOdometer >= mMilestone) {
					mOdometerUpdateListener.onReachMilestone(mOdometer);
					mMilestone += 5000;
				}
			}
			mTarget += 100;			
		}
	}

	@Override
	public void onDraw(Canvas c) {
		c.drawText(String.format("Distance%6d / %d", mOdometer / 10, mMilestone / 10), mX, mY, mPaint);
	}
	
	public interface OnOdometerUpdateListener {
		void onReachTarget(int odometer);
		void onReachMilestone(int odometer);
	}

	@Override
	public void onSizeChanged(int width, int height) {
		mPaint.setTextSize(AppScale.doScaleT(27));
	}
}
