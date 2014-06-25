package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bigbug.rocketrush.Constants;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Alient extends Barrier {

	protected final static int IMAGE_COUNT = 12; // the same size of the total number of bitmaps
	protected static boolean sImageLoaded = false;	
	protected static List<Bitmap> sImages = new ArrayList<Bitmap>();	
	protected Random mRand = new Random();
	protected boolean mSpeedUnchangeable = false;
	
	public static void loadImages(Context context, int[] resIDs) {
		if (sImageLoaded) {
			return;
		}
		sImageLoaded = true;
		
		sImages = BitmapHelper.loadBitmaps(context, resIDs);
	}
	
	public Alient(Context context) {
		super(context);
		setKind(ALIENT);
		setMovable(true);	
		setZOrder(ZOrders.ALIENT);
		loadImages(context, new int[] { R.drawable.alient01, R.drawable.alient02, R.drawable.alient03, R.drawable.alient04, R.drawable.alient05, R.drawable.alient06, R.drawable.alient07, R.drawable.alient08, R.drawable.alient09, R.drawable.alient10, R.drawable.alient11, R.drawable.alient12 });
		setWidth(sImages.get(0).getWidth());
		setHeight(sImages.get(0).getHeight());	
	}

	public void initSpeeds(float x, float y, int accTime) {		
		float accSpeedY = y / (1000 / Constants.DATA_UPDATE_INTERVAL);
		setSpeed(x, y + accSpeedY * accTime);
		setMinSpeed(x, y);
		setMaxSpeed(x, y * 3);
		setAccSpeed(0, accSpeedY);
	}

	protected int mCurIndex = 0;

	@Override
	public void onDraw(Canvas c) {
		if (mX + mWidth  <= 0 || mX >= mCanvasWidth ||
			mY + mHeight <= 0 || mY >= mCanvasHeight) {
			return; // not necessary to draw the invisible
		}
		
		if (mCurIndex == (IMAGE_COUNT << 1)) {
			mCurIndex = 0;
		}
		c.drawBitmap(sImages.get(mCurIndex++ >> 1), mX, mY, null);	
	}	

	@Override
	public void onUpdate() {
		if (!mSpeedUnchangeable) {
			if (mAccMoveDuration > 0) {
				mSpeedY = Math.min(mSpeedY + mAccSpeedY, mMaxSpeedY);
				mAccMoveDuration -= Constants.DATA_UPDATE_INTERVAL;
			} else {
				mSpeedY = Math.max(mSpeedY - mAccSpeedY, mMinSpeedY);
			}
		}

		mX += mSpeedX;
		mY += mSpeedY;		
	}

	@Override
	public void triggerCollideEffect(int kind, float x, float y) {
		float cX = mX + mWidth * 0.5f;
		float cY = mY + mHeight * 0.5f;
		float offsetY = 0;
		
		if (kind == ROCKET) {
			offsetY = 10 * mDip;
		} else if (kind == PROTECTION) {
			offsetY = 16 * mDip;
		}
				
		if (cX <= x && cY <= y - offsetY) {
			mSpeedX = -8 * mDip;
			mSpeedY = -8 * mDip;
		} else if (cX <= x && cY <= y + offsetY) {
			mSpeedX = -12 * mDip;
			mSpeedY = (mRand.nextInt(2) == 0 ? 2 : -2) * mDip;
		} else if (cX <= x && cY > y + offsetY) {
			mSpeedX = -8 * mDip;
			mSpeedY = 8 * mDip;
		} else if (cX > x && cY <= y - offsetY) {
			mSpeedX = 8 * mDip;
			mSpeedY = -8 * mDip;
		} else if (cX > x && cY <= y + offsetY) {
			mSpeedX = 12 * mDip;
			mSpeedY = (mRand.nextInt(2) == 0 ? 2 : -2) * mDip;
		} else {
			mSpeedX = 8 * mDip;
			mSpeedY = 8 * mDip;
		}
		
		mSpeedUnchangeable = true;
	}
}
