package com.bigbug.rocketrush.elements;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.basic.AppCtrl;
import com.bigbug.rocketrush.basic.AppObject;

import java.util.ArrayList;
import java.util.List;


public class Rocket extends AppObject {
	protected final static int IMAGE_COUNT = 4; // the same size of the total number of bitmaps
	protected int mCanvasWidth  = 0;
	protected int mCanvasHeight = 0;
	protected int mLeftDuration  = 0;
	protected int mRightDuration = 0;
	protected int mUpDuration    = 0;
	protected int mVibrateDuration = 0;	
	protected int mVibrateCount = 0;
	protected final static int MIN_VIBRATE_DURATION = 160;
	protected final static int MAX_VIBRATE_DURATION = 240;
	protected float mUpper  = 0;
	protected float mBottom = 0;	
	protected float mCollideArea[] = new float[4];
	public final static float DEFAULT_SPEED_X = 8;
	public final static float DEFAULT_SPEED_Y = 4;
	// rocket's area used to detect collision
	protected Rect mRect = new Rect();
	protected List<AppObject> mCollideWith = new ArrayList<AppObject>();
	// reward list notes the rewards bounding to this rocket
	protected List<Reward> mRewards = new ArrayList<Reward>();
	
	public Rocket(Context context) {
		super(context);

		setKind(ROCKET);
		setMovable(true);	
		setSpeed(DEFAULT_SPEED_X, DEFAULT_SPEED_Y);
		setMaxSpeed(DEFAULT_SPEED_X, DEFAULT_SPEED_Y);			
		setZOrder(ZOrders.ROCKET);

        loadImages(new int[]{ R.drawable.ship_1, R.drawable.ship_2, R.drawable.ship_3, R.drawable.ship_4 });
        setWidth(mImages.get(0).getWidth());
        setHeight(mImages.get(0).getHeight());
	}

	public int getAccTime() {
		return (int)((mBottom - mY) / mSpeedY);
	}
	
	public void bindReward(Reward reward) {
		if (reward.getKind() == TIMEBONUS) {
			reward.setVisible(false);
			return;
		}
		reward.setSpeed(mSpeedX, mSpeedY);
		reward.setMinSpeed(mMinSpeedX, mMinSpeedY);
		reward.setMaxSpeed(mMaxSpeedX, mMaxSpeedY);
		reward.setAccSpeed(mAccSpeedX, mAccSpeedY);
		mRewards.add(reward);
	}
	
	public void unbindReward(Reward reward) {
		mRewards.remove(reward);
	}

	protected int mCurIndex = 0;

	@Override
	public void onDraw(Canvas c) {
		if (mCurIndex == IMAGE_COUNT) {
			mCurIndex = 0;
		}
		c.drawBitmap(mImages.get(mCurIndex++), mX, mY, null);
	}
	
	@Override
	public void onUpdate() {
		if (mLeftDuration > 0) {
			mX = Math.max(mX - mSpeedX, 0);
			mLeftDuration -= Globals.DATA_UPDATE_INTERVAL;
		} else if (mRightDuration > 0) {
			mX = Math.min(mX + mSpeedX, mCanvasWidth - mWidth); 
			mRightDuration -= Globals.DATA_UPDATE_INTERVAL;
		}
		
		if (mUpDuration > 0) {
			mY = Math.max(mY - mSpeedY, mUpper);
			mUpDuration -= Globals.DATA_UPDATE_INTERVAL;
		} else {
			mY = Math.min(mY + mSpeedY, mBottom);
		}
		
		if (mVibrateDuration > 0) {					
			if (mVibrateDuration == MIN_VIBRATE_DURATION) { // first
				mVibrateCount = 0;
				mX += -3 * mDip;
			} else if (mVibrateDuration == Globals.DATA_UPDATE_INTERVAL) {
				mX += 3 * mDip;
			} else {
				mX += ((mVibrateCount & 1) == 0 ? -6 : 6) * mDip;
			}
			++mVibrateCount;
			mVibrateDuration -= Globals.DATA_UPDATE_INTERVAL;
		}
		
		mRect.left   = (int)(mX + mCollideArea[0]);
		mRect.top    = (int)(mY + mCollideArea[1]);
		mRect.right  = (int)(mX + mCollideArea[2]);
		mRect.bottom = (int)(mY + mCollideArea[3]);
	}

	@Override
	public void onSizeChanged(int width, int height) {
		mCanvasWidth  = width;
		mCanvasHeight = height;
		mX = (width - mWidth) / 2;
		mY = (height - mHeight) / 2 + height / 4;
				
		mCollideArea[0] = mWidth * 0.25f;
		mCollideArea[1] = mHeight * 0.3f;
		mCollideArea[2] = mWidth * 0.75f;
		mCollideArea[3] = mHeight * 0.5f;
	
		mUpper  = (mCanvasHeight - mHeight) * 9 / 20;
		mBottom = (mCanvasHeight - mHeight) / 2 + mCanvasHeight / 4;
		setSpeed(DEFAULT_SPEED_X * mDip, (mBottom - mUpper) / (float)(3000 / Globals.DATA_UPDATE_INTERVAL));
		setMaxSpeed(DEFAULT_SPEED_X * mDip, (mBottom - mUpper) / (float)(3000 / Globals.DATA_UPDATE_INTERVAL));
	}

	@Override
	public void operate(AppCtrl ctrl) {
		if (mMovable == false) {
			return;
		}
		
		int command = ctrl.getCommand();
		
		if (command == AppCtrl.MOVE_LEFT) {
			mLeftDuration  = Globals.DATA_UPDATE_INTERVAL;
			mRightDuration = 0;
		} else if (command == AppCtrl.MOVE_RIGHT) {
			mRightDuration = Globals.DATA_UPDATE_INTERVAL;
			mLeftDuration  = 0;
		} else if (command == AppCtrl.MOVE_VERT) {
			mUpDuration = 1000;
		} 
	}

	@Override
	public void detectCollision(List<AppObject> objects) {
		
		for (AppObject obj : objects) {
			// won't collide to itself
			if (obj == this) {
				continue;
			}
			if (!obj.getCollidable()) {
				continue;
			}

			boolean intersects = mRect.intersects(
				(int)obj.getX(), (int)obj.getY(), 
				(int)(obj.getX() + obj.getWidth()), (int)(obj.getY() + obj.getHeight()));
			if (intersects) {				
				if (obj.getKind() == PROTECTION || obj.getKind() == TIMEBONUS) {
					((Reward) obj).bindRocket(this);
				} else {
					// rocket may vibrate for a little bit of time
					mVibrateDuration = MIN_VIBRATE_DURATION;
				}
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
}
