package com.bigbug.rocketrush.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class TutorialProgress extends View
{

    /**
     * The current progress of the tutorial.
     */
    private int mPosition;

    /**
     * Paints for drawing the dots which identify the current tutorial progress.
     */
    private Paint mPaint;
    private Paint mPaintFocus;
    private Paint mShadowOuterPaint;

    public TutorialProgress(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float dip = getResources().getDisplayMetrics().density;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setARGB(80, 0, 0, 0);
        mPaint.setStyle(Paint.Style.FILL);

        mPaintFocus = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintFocus.setARGB(255, 255, 255, 255);
        mPaintFocus.setShadowLayer(4, 0, 0, Color.BLACK);
        mPaintFocus.setStyle(Paint.Style.FILL);

//        mShadowOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        mShadowOuterPaint.setMaskFilter(new BlurMaskFilter(2 * dip, BlurMaskFilter.Blur.OUTER));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        final float dip = getResources().getDisplayMetrics().density;

        int width  = getWidth();
        int height = getHeight();

        // Draw the shadow behind the dots
//        canvas.drawCircle(width / 2 - 30 * dip, height * 0.9f, 4 * dip, mShadowOuterPaint);
//        canvas.drawCircle(width / 2 - 10 * dip, height * 0.9f, 4 * dip, mShadowOuterPaint);
//        canvas.drawCircle(width / 2 + 10 * dip, height * 0.9f, 4 * dip, mShadowOuterPaint);
//        canvas.drawCircle(width / 2 + 30 * dip, height * 0.9f, 4 * dip, mShadowOuterPaint);

        // Draw the dots
        canvas.drawCircle(width / 2 - 30 * dip, height * 0.9f, 4 * dip, mPosition == 0 ? mPaintFocus : mPaint);
        canvas.drawCircle(width / 2 - 10 * dip, height * 0.9f, 4 * dip, mPosition == 1 ? mPaintFocus : mPaint);
        canvas.drawCircle(width / 2 + 10 * dip, height * 0.9f, 4 * dip, mPosition == 2 ? mPaintFocus : mPaint);
        canvas.drawCircle(width / 2 + 30 * dip, height * 0.9f, 4 * dip, mPosition == 3 ? mPaintFocus : mPaint);
    }

    public void changeTutorial(int position) {
        mPosition = position;
        invalidate();
    }
}