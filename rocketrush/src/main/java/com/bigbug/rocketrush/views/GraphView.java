package com.bigbug.rocketrush.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.bigbug.rocketrush.basic.AppPage;

public class GraphView extends SurfaceView implements SurfaceHolder.Callback {

    protected AppPage mPage;

    // constructor must have AttributeSet to create from XML
    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void setPage(AppPage page) {
        mPage = page;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            mPage.onSizeChanged(width, height);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {}

    public void surfaceDestroyed(SurfaceHolder holder) {}
}
