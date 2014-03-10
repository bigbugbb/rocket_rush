package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the app scale which is used to scale the bitmap according to the screen dimension
        Display display = getWindowManager().getDefaultDisplay();
        AppScale.calcScale(display.getWidth(), display.getHeight(), false);

        // Create the layout group and add the views
        mRootLayout = new RelativeLayout(this);
        mRootLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        mSplashView = new SplashView(this, null);
        mRootLayout.addView(mSplashView);

        // Set content view
        setContentView(mRootLayout);

        // Get the handler to perform data updating
        mHandler = Application.getUpdateHandler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean(Globals.KEY_FIRST_GAME, false).commit();
        mSplashView.release();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 11) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Start data updating
        mHandler.sendMessage(mHandler.obtainMessage(Application.MESSAGE_START_UPDATING));
        mHandler.sendMessage(mHandler.obtainMessage(Application.MESSAGE_UPDATE_DATA, new Callable<Integer>() {

            final private Object mLock = new Object();  // Object to wait

            @Override
            public Integer call() {
                int progress = mSplashView.getProgress();

                if (progress < 100) {
                    // Update splash data
                    mSplashView.updateProgress(mSplashView.getProgress() + 1);

                    synchronized (mLock) {
                        try {
                            mLock.wait(Globals.DATA_UPDATE_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    return Application.RESULT_SUCCESS;
                } else {
                    // Record the opened status
                    boolean first = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Globals.KEY_FIRST_GAME, true);
                    // Start the next activity
                    startActivity(new Intent(SplashActivity.this, first ? TutorialActivity.class : HomeActivity.class));
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_on_left);
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

            final float dip = getResources().getDisplayMetrics().density;

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setFakeBoldText(true);
            mPaint.setTextSize(AppScale.doScaleT(26));
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setShadowLayer(dip, dip, dip, getResources().getColor(R.color.dark_gray));

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
