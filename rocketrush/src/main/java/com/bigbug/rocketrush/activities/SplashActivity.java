package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.basic.AppScale;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.List;
import java.util.concurrent.Callable;

public class SplashActivity extends FragmentActivity {

    /**
     * Update handler from the application
     */
    private Handler mHandler;

    /**
     * Root layout for holding the splash view.
     */
    private RelativeLayout mRootLayout;

    /**
     * Splash view for drawing the loading animation.
     */
    private SplashView mSplashView;

    /**
     * Key to indicate whether the game has opened
     */
    private static final String KEY_GAME_OPENED = "KEY_GAME_OPENED";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupScale();

        mRootLayout = new RelativeLayout(this);
        mRootLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        mSplashView = new SplashView(this, null);
        mRootLayout.addView(mSplashView);

        setContentView(mRootLayout);

        mHandler = Application.getUpdateHandler();
    }

    @Override
    protected void onDestroy() {
        getPreferences(MODE_PRIVATE).edit().putBoolean(KEY_GAME_OPENED, true).commit();
        mSplashView.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start data updating
        mHandler.sendMessage(mHandler.obtainMessage(Application.MESSAGE_START_UPDATING));
        mHandler.sendMessage(mHandler.obtainMessage(Application.MESSAGE_UPDATE_DATA, new Callable<Integer>() {

            final private Object mLock = new Object();  // Object to wait

            @Override
            public Integer call() {
                int progress = mSplashView.getProgress();

                if (progress < 100) {
                    synchronized (mLock) {
                        try {
                            // Update splash data
                            mSplashView.updateProgress(mSplashView.getProgress() + 1);
                            mLock.wait(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    return Application.RESULT_SUCCESS;
                } else {
                    // Record the opened status
                    boolean first = getPreferences(MODE_PRIVATE).getBoolean(KEY_GAME_OPENED, true);
                    // Start the next activity
                    startActivity(new Intent(SplashActivity.this, first ? TutorialActivity.class : GameActivity.class));
                    // Close the current splash activity
                    SplashActivity.this.finish();

                    return Application.RESULT_CANCEL;
                }
            }
        }));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop data updating
        mHandler.sendMessage(mHandler.obtainMessage(Application.MESSAGE_STOP_UPDATING));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void setupScale() {
        Display display = getWindowManager().getDefaultDisplay();
        AppScale.calcScale(display.getWidth(), display.getHeight(), false);
    }

    /**
     * Splash view occupying the whole activity for displaying the animated rocket
     */
    class SplashView extends View {

        private int mNext;
        private int mProgress;
        private int mRocketWidth;

        private Paint mPaint;

        private Bitmap mBackground;
        private List<Bitmap> mRockets;

        public SplashView(Context context, AttributeSet attrs) {
            super(context, attrs);

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(0xff777777);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setTextSize(AppScale.doScaleT(24));
            mPaint.setTextAlign(Paint.Align.CENTER);

            // Load bitmaps
            mBackground = BitmapHelper.loadBitmaps(getContext(), new int[] { R.drawable.splash }).get(0);
            mRockets = BitmapHelper.loadBitmaps(getContext(), new int[] { R.drawable.ship_1_hori, R.drawable.ship_2_hori, R.drawable.ship_3_hori, R.drawable.ship_4_hori });

            mRocketWidth = mRockets.get(0).getWidth();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int width  = canvas.getWidth();
            int height = canvas.getHeight();

            // Draw the background
            final Rect rect = new Rect(0, 0, width, height);
            canvas.drawBitmap(mBackground, null, rect, null);

            // Draw the rocket
            float top  = height / 20f * 12;
            float left = (width - mRocketWidth) * (mProgress / 100f);
            canvas.drawBitmap(mRockets.get(mNext++ % 4), left, top, null);

            // Draw the loading text
            String progress = String.format("Loading...%3d%%", mProgress);
            canvas.drawText(progress, width / 2, height / 20f * 11, mPaint);
        }

        public void release() {
            if (mBackground != null) {
                mBackground.recycle();
                mBackground = null;
            }
            for (Bitmap bitmap : mRockets) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
            mRockets = null;
        }

        public int getProgress() {
            return mProgress;
        }

        public void updateProgress(int progress) {
            mProgress = progress;

            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run()
                {
                    invalidate();
                }
            });
        }
    }
}
