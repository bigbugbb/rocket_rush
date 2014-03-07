package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.ArrayList;
import java.util.List;

public class Bird extends Barrier {

	protected static boolean sImageLoaded = false;
	protected static List<Bitmap> sImages = new ArrayList<Bitmap>();
	protected Bitmap mImage = null;		

	public static void loadImages(Context context, int[] resIDs) {
		if (sImageLoaded) {
			return;
		}
		sImageLoaded = true;

        sImages = BitmapHelper.loadBitmaps(context, resIDs);
	}
	
	public Bird(Context context, boolean right) {
		super(context);
		loadImages(context, new int[] { R.drawable.bird_1, R.drawable.bird_2 });
		setKind(BIRD);
		setMovable(true);	
		setZOrder(ZOrders.BIRD);		
		setImage(sImages.get(right ? 1 : 0));
	}
	
	public void initSpeeds(float x, float y, int accTime) {		
		float accSpeedY = y / (1000 / Globals.DATA_UPDATE_INTERVAL);
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
		c.drawBitmap(mImage, mX, mY, null);
	}	

	@Override
	public void onUpdate() {
		if (mAccMoveDuration > 0) {
			mSpeedY = Math.min(mSpeedY + mAccSpeedY, mMaxSpeedY);
			mAccMoveDuration -= Globals.DATA_UPDATE_INTERVAL;
		} else {
			mSpeedY = Math.max(mSpeedY - mAccSpeedY, mMinSpeedY);
		}
		
		mX += mSpeedX;
		mY += mSpeedY;		
	}

	@Override
	public void triggerCollideEffect(int kind, float x, float y) {
		mSpeedX = -mSpeedX;
		setImage(sImages.get(mSpeedX > 0 ? 1 : 0));
	}

}
