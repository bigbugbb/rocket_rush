package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.basic.AppObject;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.ArrayList;
import java.util.List;

public class TimeBonus extends Reward {
	protected final static int IMAGE_COUNT = 2; // the same size of the total number of bitmaps
	protected final static int IMAGE_UNBOUND_START = 0; 
	protected final static int IMAGE_BOUND_START   = 0;
	protected static boolean sImageLoaded = false;	
	protected static List<Bitmap> sImages = new ArrayList<Bitmap>();
	// time bonus value
	protected int mBonus = 12;
	// the difference of the top left point between this field and the rocket 
	protected float mOffsetX = 0;
	protected float mOffsetY = 0;	
	// save for objects collided with this field
	protected List<AppObject> mCollideWith = new ArrayList<AppObject>();

	public static void loadImages(Context context, int[] resIDs) {
		if (sImageLoaded) {
			return;
		}
		sImageLoaded = true;
		
		sImages = BitmapHelper.loadBitmaps(context, resIDs);
	}
	
	public TimeBonus(Context context) {
		super(context);
		loadImages(context, new int[] { R.drawable.single_time_bonus_1, R.drawable.single_time_bonus_2 });
		setKind(TIMEBONUS);
		setZOrder(ZOrders.TIMEBONUS);
		setWidth(sImages.get(IMAGE_UNBOUND_START).getWidth());
		setHeight(sImages.get(IMAGE_UNBOUND_START).getHeight());	
	}

    public int getBonusTime() {
        return mBonus;
    }

	private int mUpdateUnbound = 0;
	@Override
	protected void updateUnbound() {
		if (++mUpdateUnbound > 32) {
			mUpdateUnbound = 0;
		}
	
		mX += mSpeedX;
		mY += mSpeedY;		
		
		if (isTimeout()) {
			return; // the reward will fly out of the screen
		}
		
		if (mX < 0) {
			mSpeedX = Math.abs(mSpeedX);
		} else if (mX > mCanvasWidth - mWidth) {
			mSpeedX = -Math.abs(mSpeedX);
		}		
		if (mY < 0) {
			mSpeedY = Math.abs(mSpeedY);
		} else if (mY > mCanvasHeight - mHeight) {
			mSpeedY = -Math.abs(mSpeedY);
		}	
	}

	protected int mUnboundIndex = 0;

	@Override
	protected void drawUnbound(Canvas c) {
		if (mX + mWidth  <= 0 || mX >= mCanvasWidth ||
			mY + mHeight <= 0 || mY >= mCanvasHeight) {
			return; // not necessary to draw the invisible
		}
		c.drawBitmap(sImages.get(mUpdateUnbound <= 16 ? 0 : 1), mX, mY, null);	
	}
	
	@Override
	public void detectCollision(List<AppObject> objects) {
		
		for (AppObject obj : objects) {
			// won't collide to itself
			if (obj == this) {
				continue;
			}
			int kind = obj.getKind();
			if (!obj.getCollidable() || kind == ROCKET || kind == PROTECTION || kind == TIMEBONUS) {
				continue;
			}			

			if (isCollidedWith(obj)) {				
				mCollideWith.add(obj);
			}
		}				
		
		if (mCollideWith.size() > 0) {
			if (mOnCollideListener != null) {
				mOnCollideListener.onCollide(this, mCollideWith);				
			}
			for (AppObject obj : mCollideWith) {
				obj.setCollidable(false);				
			}
			mCollideWith.clear();
		}
	}
	
	protected boolean isCollidedWith(AppObject obj) {
		float centerX = mX + mWidth * 0.5f;
		float centerY = mY + mHeight * 0.5f;
		float centerObjX = obj.getX() + obj.getWidth() * 0.5f;
		float centerObjY = obj.getY() + obj.getHeight() * 0.5f;
		
		if (Math.pow((centerX - centerObjX), 2f) + Math.pow((centerY - centerObjY), 2f) < 
			Math.pow((mWidth + obj.getWidth()) * 0.5f, 2))			
			return true;
		
		return false;
	}

	@Override
	protected void onBound() {
		if (mListener != null) {
			mListener.onGotReward(this);
		}
		
		// make it out of the screen, so it can be recycled
		mX = 10000;
		mY = 10000;
	}
}
