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


public class Asteroid extends Barrier {

	protected final static int ASTEROID_COUNT = 12; // the same size of the total number of bitmaps
	protected final static int EXPLODING_ASTEROID_COUNT = 4;
	protected static boolean sImageLoaded = false;
	protected static List<Bitmap> sImages = new ArrayList<Bitmap>();
	protected Bitmap mImage = null;		
	protected Random mRand = new Random();
	protected boolean mSpeedUnchangeable = false;
	protected boolean mExplode = false;
	protected int mExplodeIndex = 0;
	
	public static void loadImages(Context context, int[] resIDs) {
		if (sImageLoaded) {
			return;
		}
		sImageLoaded = true;
		
		sImages = BitmapHelper.loadBitmaps(context, resIDs);
	}
	
	public Asteroid(Context context) {
		super(context);
		loadImages(context, new int[] { R.drawable.asteroid01, R.drawable.asteroid02, R.drawable.asteroid03, R.drawable.asteroid04, R.drawable.asteroid05, R.drawable.asteroid06, R.drawable.asteroid07, R.drawable.asteroid08, R.drawable.asteroid09, R.drawable.asteroid10, R.drawable.asteroid11, R.drawable.asteroid12, R.drawable.asteroid_explode1, R.drawable.asteroid_explode2, R.drawable.asteroid_explode3, R.drawable.asteroid_explode4 });
		setKind(ASTEROID);
		setMovable(true);	
		setZOrder(ZOrders.ASTEROID);		
		setImage(sImages.get(mRand.nextInt(ASTEROID_COUNT)));
	}
//
//	public Asteroid(Context context, Bitmap image) {
//		super(context);
//		loadImages(context, new int[] { R.drawable.asteroid01, R.drawable.asteroid02, R.drawable.asteroid03, R.drawable.asteroid04, R.drawable.asteroid05, R.drawable.asteroid06, R.drawable.asteroid07, R.drawable.asteroid08, R.drawable.asteroid09, R.drawable.asteroid10, R.drawable.asteroid11, R.drawable.asteroid12, R.drawable.asteroid_explode1, R.drawable.asteroid_explode2, R.drawable.asteroid_explode3, R.drawable.asteroid_explode4 });
//		setKind(ASTEROID);
//		setMovable(true);
//		setZOrder(ZOrders.ASTEROID);
//		setImage(sImages.get(mRand.nextInt(ASTEROID_COUNT)));
//	}
//
	public void initSpeeds(float x, float y, int accTime) {		
		float accSpeedY = y / (1000 / Constants.DATA_UPDATE_INTERVAL);
		setSpeed(x, y + accSpeedY * accTime);
		setMinSpeed(x, y);
		setMaxSpeed(x, y + y + y);
		setAccSpeed(0, accSpeedY);
	}

	public void setImage(Bitmap image) {
		mImage = image;
		setWidth(image.getWidth());
		setHeight(image.getHeight());
	}

	@Override
	public void onDraw(Canvas c) {
		if (mX + mWidth  <= 0 || mX >= mCanvasWidth ||
			mY + mHeight <= 0 || mY >= mCanvasHeight) {
			return; // not necessary to draw the invisible
		}
		
		if (mExplode) {
			c.drawBitmap(sImages.get(mExplodeIndex++), mX, mY, null);
			if (mExplodeIndex >= ASTEROID_COUNT + EXPLODING_ASTEROID_COUNT) {
				mExplode = false;
				mVisible = false;
			}
		}
		if (mVisible) {
			c.drawBitmap(mImage, mX, mY, null);
		}
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
		if (kind == ROCKET) {
			float cX = mX + mWidth * 0.5f;
			float cY = mY + mHeight * 0.5f;
			float offsetY = 10 * mDip;
					
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
		} else if (kind == PROTECTION) {
			setZOrder(ZOrders.EFFECTS);
			mExplode = true;
			mExplodeIndex = ASTEROID_COUNT;
		}
	}

}
