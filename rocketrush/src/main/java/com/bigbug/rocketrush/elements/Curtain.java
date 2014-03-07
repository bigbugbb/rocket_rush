package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;

import com.bigbug.rocketrush.Globals;

public class Curtain extends Utility {

	protected Paint mPaint = null;
	protected Paint mPaintText = null;
	protected float mUpper  = 0;
	protected float mBottom = 0;
	protected float mDelta  = 0;
	protected int mCloseDuration = 0;
	protected int mDelayDuration = 0;
	protected int mOpenDuration  = 0;
	protected OnCurtainEventListener mListener = null;
	
	public Curtain(Context context) {
		super(context);
		setKind(CURTAIN);
		setMovable(false);
		setCollidable(false);
		setZOrder(ZOrders.CURTAIN);
		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setColor(Color.DKGRAY);
		mPaint.setStyle(Style.FILL);
		
		mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaintText.setColor(Color.WHITE);
		mPaintText.setStyle(Style.FILL);
		mPaintText.setTypeface(Typeface.SERIF);
		mPaintText.setFakeBoldText(true);
		mPaintText.setTextSize(40);
		mPaintText.setShadowLayer(4, 1, 1, Color.DKGRAY);
		mPaintText.setTextAlign(Paint.Align.CENTER);
	}
	
	public void setCurtainEventListener(OnCurtainEventListener listener) {
		mListener = listener;
	}
	
	public void setDelay(int delay) {
		mDelayDuration = delay;
	}
	
	public void close() {
		mCloseDuration = 1000;
		if (mListener != null) {
			mListener.onCurtainPreClosing();
		}
	}
	
	public void open() {
		mOpenDuration = 1000;
		if (mListener != null) {
			mListener.onCurtainPreOpening();
		}
	}

	@Override
	public void onUpdate() {
		if (mCloseDuration > 0) {
			mBottom += mDelta;
			mUpper  -= mDelta;
			mCloseDuration -= Globals.DATA_UPDATE_INTERVAL;
			if (mUpper <= mBottom) {
				mCloseDuration = 0;
				mDelayDuration = 500;
				if (mListener != null) {
					mListener.onCurtainClosed();
				}
			}
		} else if (mDelayDuration > 0) {
			mDelayDuration -= Globals.DATA_UPDATE_INTERVAL;
		} else if (mOpenDuration > 0) {
			mBottom -= mDelta;
			mUpper  += mDelta;
			mOpenDuration -= Globals.DATA_UPDATE_INTERVAL;
			if (mBottom <= 0 && mUpper >= mHeight) {
				mOpenDuration = 0;
				if (mListener != null) {
					mListener.onCurtainOpened();
				}
			}
		}
	}

	@Override
	public void onDraw(Canvas c) {
		if (mCloseDuration == 0 && mDelayDuration == 0 && mOpenDuration == 0) {
			return;
		}
		c.drawRect(0, 0, mWidth, mBottom, mPaint);
		c.drawRect(0, mUpper, mWidth, mHeight, mPaint);
		if (mDelayDuration > 0) {
			c.drawText("Next Loop", mWidth / 2, mHeight / 2, mPaintText);
		}
	}

	@Override
	public void onSizeChanged(int width, int height) {
		mWidth  = width;
		mHeight = height;
		
		mBottom = 0;
		mUpper  = mHeight;		
		mDelta  = mHeight / (1000f / Globals.DATA_UPDATE_INTERVAL / 2);
	}

	public interface OnCurtainEventListener {
		void onCurtainPreClosing();
		void onCurtainClosed();
		void onCurtainPreOpening();
		void onCurtainOpened();
	}
}
