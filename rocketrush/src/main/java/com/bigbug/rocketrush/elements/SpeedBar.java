package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.basic.AppCtrl;

public class SpeedBar extends Utility {

	protected Bitmap mImage;

	// canvas size
	protected int mCanvasWidth;
	protected int mCanvasHeight;

	// bottom
	protected float mBarTop;
	protected float mBarBottom;

	// bad name, but short : P
	protected Paint mPaint1 = new Paint();
	protected Paint mPaint2 = new Paint(); 
	protected Paint mPaint3 = new Paint();
	protected Paint mPaintShadow = new Paint();
	protected RectF r1 = new RectF();
	protected RectF r2 = new RectF();
	protected RectF r3 = new RectF();
	protected RectF rShadow = new RectF();

	// duration for move up, the same as Rocket
	protected int mUpDuration = 0;

	public SpeedBar(Context context) {
		super(context);

        mDip = context.getResources().getDisplayMetrics().density;

		setKind(SPEEDBAR);
		setSpeed(0, mDip);
		setCollidable(false);
		setZOrder(ZOrders.SPEEDBAR);
		
		mPaint1.setAntiAlias(true);
		mPaint1.setARGB(180, 0, 0, 0);                 
		mPaint1.setStrokeWidth(3.0f * mDip);
		mPaint1.setStyle(Style.STROKE);
		
		mPaint2.setAntiAlias(true);		    
		mPaint2.setARGB(100, 255, 255, 255);
		mPaint2.setStyle(Style.FILL);
		
		mPaint3.setAntiAlias(true);		    
		mPaint3.setARGB(255, 86, 217, 7);
		mPaint3.setStyle(Style.FILL);
		
		mPaintShadow.setARGB(70, 0, 0, 0);
		mPaintShadow.setStrokeWidth(3.0f * mDip);
		mPaintShadow.setStyle(Style.STROKE);
		mPaintShadow.setAntiAlias(true);
	}

	public void setImage(Bitmap image) {
		mImage = image;
	}

	@Override
	public void onUpdate() {
		if (mUpDuration > 0) {
			mY = Math.max(mY - mSpeedY, mBarTop);
			mUpDuration -= Globals.DATA_UPDATE_INTERVAL;
		} else {
			mY = Math.min(mY + mSpeedY, mBarBottom);
			mUpDuration = 0;
		} 
		r3.top = mY;
	}
	
	@Override
	public void onDraw(Canvas c) {
		c.drawRoundRect(rShadow, 15 * mDip, 15 * mDip, mPaintShadow);
		c.drawRoundRect(r1, 12 * mDip, 12 * mDip, mPaint1);
	    c.drawRoundRect(r2, 9 * mDip, 9 * mDip, mPaint2);
	    c.drawRoundRect(r3, 9 * mDip, 9 * mDip, mPaint3);
	}

	// too many magic numbers in the class, I will modify later. :P
	@Override
	public void onSizeChanged(int width, int height) {
		mCanvasWidth  = width;
		mCanvasHeight = height;
		
		r1.left   = 30 * mDip;
	    r1.top    = mCanvasHeight * 0.58f;
	    r1.right  = 42 * mDip;
	    r1.bottom = mCanvasHeight * 0.9f;
	    
	    r2.left   = 31 * mDip;
	    r2.top    = mCanvasHeight * 0.58f + 2 * mDip;
	    r2.right  = 41 * mDip;
	    r2.bottom = mCanvasHeight * 0.9f - 2 * mDip;
	    
	    r3.left   = 31 * mDip;
	    r3.top    = mCanvasHeight * 0.9f - 2 * mDip;
	    r3.right  = 41 * mDip;
	    r3.bottom = mCanvasHeight * 0.9f - 2 * mDip;
	    
	    rShadow.left   = 26 * mDip;
	    rShadow.top    = mCanvasHeight * 0.58f - 4 * mDip;
	    rShadow.right  = 46 * mDip;
	    rShadow.bottom = mCanvasHeight * 0.9f + 4 * mDip;
	    
	    mBarTop    = mCanvasHeight * 0.58f + 2 * mDip;
	    mBarBottom = mCanvasHeight * 0.9f - 2 * mDip;
	    mY = mBarBottom;
	    setSpeed(0, (mBarBottom - mBarTop) / (3000f / Globals.DATA_UPDATE_INTERVAL));
	}

	@Override
	public void operate(AppCtrl ctrl) {
		if (!mEnable) {
			return;
		}
		int command = ctrl.getCommand();

		if (command == AppCtrl.MOVE_VERT) {
			mUpDuration = 1000;
		}
	}
}
