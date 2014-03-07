package com.bigbug.rocketrush.basic;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AppPage implements GestureDetector.OnGestureListener
{
    // app context
    protected Context mContext;
    // message handler
    protected Handler mHandler;
    // flag to indicate whether this page is enabled
    protected boolean mEnable;
    // size of the page
    protected int mWidth;
    protected int mHeight;
    // UI or game objects
    protected List<AppObject> mObjects;
    // gesture detector
    protected GestureDetector mGestureDetector;
    // the current selected object
    protected AppObject mSelObject;
    // the last selected object
    protected AppObject mLastSelObject;

    protected AppPage(Context context) {
        mContext = context;
        mEnable  = true;
        mWidth   = 0;
        mHeight  = 0;

        mGestureDetector = new GestureDetector(this);
        mObjects = new ArrayList<AppObject>();

        mSelObject     = null;
        mLastSelObject = null;
    }

    public void resume() {
    }

    public void pause() {
    }

    public void start() {
    }

    public void stop() {
    }

    public void reset() {
    }

    public void restart() {
        pause();
        stop();
        reset();
        start();
        resume();
    }

    public void release() {
        for (AppObject obj : mObjects) {
            try {
                obj.release();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        mObjects.clear();
        System.gc();
    }

    public Context getContext() {
        return mContext;
    }

    public void setEnable(boolean enable) {
        mEnable = enable;
    }

    public boolean isEnabled() {
        return mEnable;
    }

    public void onAppEvent(AppEvent e) {
    }

    protected void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void onSizeChanged(int width, int height) {
        mWidth  = width;
        mHeight = height;

        for (AppObject obj : mObjects) {
            try {
                obj.onSizeChanged(width, height);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public void onUpdate() {
        for (AppObject obj : mObjects) {
            obj.onUpdate();
        }
    }

    public void onDraw(Canvas c) {
        for (AppObject obj : mObjects) {
            obj.onDraw(c);
        }
    }

    protected void orderByZ(List<AppObject> objects) {
        Collections.sort(objects, new ZOrderComparator());
    }

    private class ZOrderComparator implements Comparator<AppObject>
    {
        public int compare(AppObject obj1, AppObject obj2) {
            return obj1.getZOrder() - obj2.getZOrder();
        }
    }

    // onTouch should not be handled simultaneously with onDraw
    public synchronized boolean onTouch(MotionEvent e) {
        boolean ret = false;

        if (mGestureDetector.onTouchEvent(e)) {
            return true;
        } else {
            switch (e.getAction()) {
                case MotionEvent.ACTION_UP:
                    ret = onUp(e);
                    break;
                default:
                    ret = false;
                    break;
            }
        }

        return ret;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        boolean ret = false;
        float x = e.getX();
        float y = e.getY();

        for (int i = mObjects.size() - 1; i >= 0; --i) {
            if (mObjects.get(i).contains(x, y)) {
                mSelObject = mObjects.get(i);
                mLastSelObject = mSelObject;
                mSelObject.setSelected(true);
                ret = mSelObject.onDown(e);
                break;
            }
        }

        return ret;
    }

    public boolean onUp(MotionEvent e) {
        boolean ret = false;

        if (mSelObject != null) {
            ret = mSelObject.onUp(e);
            mSelObject.setSelected(false);
            mSelObject = null;
        }

        return ret;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        boolean ret = false;

        if (mSelObject != null) {
            if (mSelObject.contains(e2.getX(), e2.getY())) {
                ret = mSelObject.onFling(e1, e2, velocityX, velocityY);
            }
        }

        return ret;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        boolean ret = false;

        if (mSelObject != null) {
            if (mSelObject.contains(e2.getX(), e2.getY())) {
                ret = mSelObject.onScroll(e1, e2, distanceX, distanceY);
            } else {
                Log.d("scroll", "out of range");
                mSelObject.onCancelSelection(e2);
                mSelObject.setSelected(false);
                mSelObject = null;
                ret = true;
            }
        }

        return ret;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        return false; // do nothing here
    }
}