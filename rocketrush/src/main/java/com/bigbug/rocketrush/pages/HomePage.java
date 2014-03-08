package com.bigbug.rocketrush.pages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.basic.AppPage;
import com.bigbug.rocketrush.utils.BitmapHelper;
import com.bigbug.rocketrush.utils.MusicPlayer;

import java.util.List;

public class HomePage extends AppPage {

    private float mX;

    private float mY;

    private Rect mRect;

    private List<Bitmap> mBitmaps;

    private MusicPlayer mMusicPlayer;

    public HomePage(Context context) {
        super(context);
        mMusicPlayer = new MusicPlayer(context);
    }

    @Override
    public void create() {
        super.create();

        mBitmaps = BitmapHelper.loadBitmaps(getContext(), new int[] { R.drawable.home_background, R.drawable.home_cloud });

        mMusicPlayer.create(R.raw.bkg_music_1);
    }

    @Override
    public void destroy() {
        super.destroy();

        mMusicPlayer.destroy();

        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
        mBitmaps = null;
    }

    @Override
    public void start() {
        mMusicPlayer.play();
    }

    @Override
    public void stop() {
        mMusicPlayer.pause();
    }

    @Override
    public void reset() {
        mMusicPlayer.reset();
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

    public MusicPlayer getMusicPlayer() {
        return mMusicPlayer;
    }
}
