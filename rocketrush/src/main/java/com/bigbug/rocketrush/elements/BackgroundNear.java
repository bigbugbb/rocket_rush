package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bigbug.rocketrush.Constants;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.basic.AppCtrl;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.ArrayList;
import java.util.List;


public class BackgroundNear extends Background {

	protected final static int BACKGROUND_COUNT = 3; // the same size of the total number of bitmaps
	protected static boolean sImageLoaded = false;	
	protected static List<Bitmap> sImages = new ArrayList<Bitmap>();	
	
	private int mAccMoveDuration = 0;
	public final static float DEFAULT_SPEED_X = 0;
	public final static float DEFAULT_SPEED_Y = 3f;
	
	protected boolean mSwitching[] = { false, false };
	protected int mImageIndex[] = { 0, 0 };
	
	public static void loadImages(Context context, int[] resIDs) {
		if (sImageLoaded) {
			return;
		}
		sImageLoaded = true;
		
		sImages = BitmapHelper.loadBitmaps(context, resIDs);
	}
	
	public BackgroundNear(Context context) {
		super(context);
		setSpeed(DEFAULT_SPEED_X, DEFAULT_SPEED_Y * mDip);
		setMaxSpeed(0, DEFAULT_SPEED_Y * 3 * mDip);
		setAccSpeed(0, DEFAULT_SPEED_Y * mDip / (1000 / Constants.DATA_UPDATE_INTERVAL));
		setZOrder(ZOrders.BACKGROUND_NEAR);

		loadImages(context, new int[] { R.drawable.bg1_near, R.drawable.bg2_near, R.drawable.bg3_near });
		setWidth(sImages.get(0).getWidth());
		setHeight(sImages.get(0).getHeight());
	}
	
	public void switchToFirst() {
		mY = 0;		
		mImageIndex[0] = 0;
		mImageIndex[1] = 0;
	}
	
	public void switchToNext() {
		mSwitching[0] = true;
		mSwitching[1] = true;
	}

	@Override
	public void onSizeChanged(int width, int height) {
		for (int i = 0; i < BACKGROUND_COUNT; ++i) {
			float radio = sImages.get(i).getHeight() / (float) sImages.get(i).getWidth();	
			int scaledWidth  = width;
			int scaledHeight = (int)(width * radio);
			
			if (scaledWidth == sImages.get(i).getWidth() && 
				scaledHeight == sImages.get(i).getHeight()) {
				continue;
			}
			
			Bitmap newImage = 
				Bitmap.createScaledBitmap(sImages.get(i), scaledWidth, scaledHeight, true);	
			sImages.get(i).recycle(); // explicit call to avoid out of memory
			sImages.set(i, newImage);
			
			System.gc();
		}
		mWidth  = sImages.get(0).getWidth();
		mHeight = sImages.get(0).getHeight();
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
		if (mAccMoveDuration > 0) {
			mSpeedY = Math.min(mSpeedY + mAccSpeedY, mMaxSpeedY);
			mY += mSpeedY;
			mAccMoveDuration -= Constants.DATA_UPDATE_INTERVAL;
		} else {
			mSpeedY = Math.max(mSpeedY - mAccSpeedY, DEFAULT_SPEED_Y);
			mY += mSpeedY;
		}
	}

	@Override
	public void onDraw(Canvas c) {
		int maxHeight = sImages.get(mImageIndex[1]).getHeight();
		
		if (mY >= maxHeight) {
			mY = 0;
			if (mSwitching[0]) { // we need to draw the old first, so update mImageIndex[1]
				mImageIndex[1] = Math.min(mImageIndex[1] + 1, BACKGROUND_COUNT - 1);
				mSwitching[0] = false;
			} else if (mSwitching[1]) { // the old image has drawn up, update mImageIndex[0] to the new
				mImageIndex[0] = Math.min(mImageIndex[0] + 1, BACKGROUND_COUNT - 1);
				mSwitching[1] = false;
			}			
			c.drawBitmap(sImages.get(mImageIndex[0]), mX, mY, null);
		} else {
			c.drawBitmap(sImages.get(mImageIndex[1]), mX, mY - maxHeight, null);		
			c.drawBitmap(sImages.get(mImageIndex[0]), mX, mY, null);
		}
	}

}
