package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.basic.AppScale;

public class Timer extends Utility {

	protected Paint mPaint = null;
	protected int mTime = Globals.GAME_TIME;
	protected int mOneSecond = 1000;
	protected String mTextTime = "Time " + mTime;
	protected int mCanvasWidth  = 0;
	protected int mCanvasHeight = 0;
	protected OnTimeUpdateListener mTimeUpdateListener = null;
	
	public Timer(Context context) {
		super(context);

		setKind(TIMER);
		setMovable(false);
		setCollidable(false);
		setZOrder(ZOrders.TIMER);
		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setColor(Color.WHITE);
		mPaint.setStyle(Style.FILL);
		mPaint.setTextSize(AppScale.doScaleT(30));
		mPaint.setFakeBoldText(true);
		mPaint.setShadowLayer(2 * mDip, mDip, mDip, Color.BLACK);
	}
	
	public void setOnTimeUpdateListener(OnTimeUpdateListener listener) {
		mTimeUpdateListener = listener;
	}

	public void addBonusTime(int bonus) {
		mTime += bonus;
	}
	
	@Override
	public void onUpdate() {
		if (!mEnable) {
			return;
		}
		if (mOneSecond == 0) {
			mOneSecond = 1000;
			mTime = Math.max(mTime - 1, 0);
			mTextTime = "Time " + (mTime < 10 ? " " : "") + mTime;
			if (mTimeUpdateListener != null) {
				mTimeUpdateListener.onTimeUpdate(mTime);
			}
		}
		mOneSecond -= Globals.DATA_UPDATE_INTERVAL;

		super.onUpdate();
	}		

	protected int mHalfSecond = 500;

	@Override
	public void onDraw(Canvas c) {
		if (mHalfSecond == 0) {
			mHalfSecond = 500;
			if (mTime == 0) {
				mPaint.setColor(Color.WHITE);
			} else if (mTime > 0 && mTime < 10) {
				if (mPaint.getColor() == Color.WHITE) {
					mPaint.setColor(Color.RED);
				} else {
					mPaint.setColor(Color.WHITE);
				}
			} else if (mTime >= 10) {
				mPaint.setColor(Color.WHITE);
			}
		}
		mHalfSecond -= Globals.DATA_UPDATE_INTERVAL;
		c.drawText(mTextTime, mCanvasWidth - mPaint.measureText(mTextTime) - 24 * mDip, 40 * mDip, mPaint);
	}

	@Override
	public void onSizeChanged(int width, int height) {
		mCanvasWidth  = width;
		mCanvasHeight = height;
		
		mPaint.setTextSize(AppScale.doScaleT(32));
	}
	
	public interface OnTimeUpdateListener {
		void onTimeUpdate(int curTime);
	}
}
