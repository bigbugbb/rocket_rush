package com.bigbug.rocketrush.sdktest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.bigbug.rocketrush.R;


public class DeleteButton extends View {

    // Paint for drawing the button
    private Paint mPaint;

    // Center of the button circle
    private float mCenterX;
    private float mCenterY;

    // Radius of the button outer circle
    private float mRadius;

    // Offset of the cross line in the circle for the starting and ending points
    private float mOffset;

    // Stroke with of the cross lines in the circle
    private float mStrokeWidth;

    // Radius of the button inner circle
    private float mInnerRadius;

    // Bitmap of the button
    private Bitmap mBitmap;

    // Bit map of the shadow behind the button
    private Bitmap mShadowBitmap;

    public DeleteButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        final float dip = getResources().getDisplayMetrics().density;

        mCenterX     = 16 * dip;
        mCenterY     = 16 * dip;
        mRadius      = 16 * dip;
        mOffset      = 8 * dip;
        mStrokeWidth = 3 * dip;
        mInnerRadius = mRadius - mStrokeWidth * 0.5f;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        setLayoutParams(new RelativeLayout.LayoutParams((int) (34 * dip + 0.5f), (int) (34 * dip + 0.5f)));

        // Create the button bitmap and the canvas in which the button bitmap will be drawn
        mBitmap = Bitmap.createBitmap((int) (32 * dip + 0.5f), (int) (32 * dip + 0.5f), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);

        // Draw the outer circle
        mPaint.setColor(getResources().getColor(R.color.company_color));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);

        // Draw the inner circle
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mPaint);

        // Draw the cross lines in the circle
        mPaint.setStrokeWidth(5f * dip);
        canvas.drawLine(mCenterX - mOffset, mCenterY, mCenterX + mOffset, mCenterY, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        final float dip = getResources().getDisplayMetrics().density;

        canvas.drawBitmap(mBitmap, (getWidth() - mBitmap.getWidth()) / 2, (getHeight() - mBitmap.getHeight()) / 2, mPaint);
    }

    public void release()
    {
        if (null != mBitmap)
        {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
