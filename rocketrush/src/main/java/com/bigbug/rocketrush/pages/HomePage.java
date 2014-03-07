package com.bigbug.rocketrush.pages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.basic.AppPage;
import com.bigbug.rocketrush.media.BackgroundMusic;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.List;

public class HomePage extends AppPage {

    private float mX;

    private float mY;

    private Rect mRect;

    private List<Bitmap> mBitmaps;

    protected BackgroundMusic mBackgroundMusic = BackgroundMusic.getInstance();

    public HomePage(Context context) {
        super(context);
    }

    @Override
    public void start() {
        mBitmaps = BitmapHelper.loadBitmaps(getContext(), new int[] { R.drawable.home_background, R.drawable.home_cloud });

        mBackgroundMusic.create(mContext, R.raw.bkg_music_1);
        mBackgroundMusic.play();
    }

    @Override
    public void stop() {
        mBackgroundMusic.pause();
        mBackgroundMusic.stop();

        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
        mBitmaps = null;
    }

    @Override
    public void reset() {
        mBackgroundMusic.reset();
    }

    @Override
    public void onUpdate() {
        mX += mContext.getResources().getDisplayMetrics().density * 1.5f;
        if (mX >= mWidth) {
            mX = 0;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmaps.get(0), null, mRect, null);

        if (mX >= mWidth) {
            canvas.drawBitmap(mBitmaps.get(1), mX, mY, null);
        } else {
            canvas.drawBitmap(mBitmaps.get(1), mX - mWidth, mY, null);
            canvas.drawBitmap(mBitmaps.get(1), mX, mY, null);
        }
    }

    @Override
    public void onSizeChanged(int width, int height) {
        mWidth  = width;
        mHeight = height;

        mX = width / 2;
        mY = 0;

        mRect = new Rect(0, 0, mWidth, mHeight);
    }
}
