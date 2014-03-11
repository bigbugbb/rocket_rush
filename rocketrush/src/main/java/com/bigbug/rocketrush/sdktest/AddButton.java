package com.bigbug.rocketrush.sdktest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.bigbug.rocketrush.R;

/**
 * Created by jefflopes on 3/11/14.
 */
public class AddButton extends View {

    // Paint for drawing the button
    private Paint mPaint;

    // Paint for text
    private Paint mTextPaint;

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

    // Text boundary
    private Rect mTextRect = new Rect();

    // Text string
    private static final String TEXT = "More...";

    public AddButton(Context context, AttributeSet attrs)
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
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics()));
        mTextPaint.getTextBounds(TEXT, 0, String.format(TEXT).length(), mTextRect);

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
        canvas.drawLine(mCenterX, mCenterY - mOffset, mCenterX, mCenterY + mOffset, mPaint);
        canvas.drawLine(mCenterX - mOffset, mCenterY, mCenterX + mOffset, mCenterY, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        final float dip = getResources().getDisplayMetrics().density;

        canvas.drawColor(getResources().getColor(R.color.title_color));
        float width = mTextRect.width() + mBitmap.getWidth() + 8 * dip;
        canvas.drawBitmap(mBitmap, (getWidth() - width) / 2, (getHeight() - mBitmap.getHeight()) / 2, mPaint);
        canvas.drawText(TEXT, (getWidth() - width) / 2 + mBitmap.getWidth() + 8 * dip, mTextRect.height() + (getHeight() - mTextRect.height()) / 2, mTextPaint);
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
