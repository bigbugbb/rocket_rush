package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.ArrayList;
import java.util.List;

public class Thunder extends Barrier {

	protected static boolean sImageLoaded = false;
	protected static List<Bitmap> sImages = new ArrayList<Bitmap>();	
	protected int mPeriod = 0;
	
	public static void loadImages(Context context, int[] resIDs) {
		if (sImageLoaded) {
			return;
		}
		sImageLoaded = true;

		sImages = BitmapHelper.loadBitmaps(context, resIDs);
	}

	public Thunder(Context context) {
		super(context);
		setKind(THUNDER);
		setMovable(true);	
		setVisible(false);
		setCollidable(false);
		setZOrder(ZOrders.THUNDER);

		loadImages(context, new int[] { R.drawable.thunder });
		setWidth(sImages.get(0).getWidth());
		setHeight(sImages.get(0).getHeight());	
	}

	public void initSpeeds(float x, float y, int accTime) {					
		setSpeed(x, y);
		setMinSpeed(x, y);
		setMaxSpeed(x, y);
		setAccSpeed(0, 0);
	}

	@Override
	public void onDraw(Canvas c) {
		if (mX + mWidth  <= 0 || mX >= mCanvasWidth ||
			mY + mHeight <= 0 || mY >= mCanvasHeight) {
			return; // not necessary to draw the invisible
		}
		
		if (mVisible && (mPeriod & 3) != 0) {		
			c.drawBitmap(sImages.get(0), mX, mY, null);			
		}
	}	

	@Override
	public void onUpdate() {
		if (++mPeriod == 60) {
			mVisible = true;
			mCollidable = true;
		} else if (mPeriod == 100) {
			mPeriod = 0;
			mVisible = false;
			mCollidable = false;
		}
		mY += mSpeedY;		
	}
}
