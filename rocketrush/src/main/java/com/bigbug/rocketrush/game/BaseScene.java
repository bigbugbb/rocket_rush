package com.bigbug.rocketrush.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

import com.bigbug.rocketrush.basic.AppObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BaseScene extends AppObject implements AppObject.OnCollideListener {

    protected int mWidth;
    protected int mHeight;

    protected List<AppObject> mObjects;

    protected GameEvent.OnGameEventListener mListener;

    public BaseScene(Context context) {
        super(context);
        mObjects  = new ArrayList<AppObject>();
    }

    public void setOnGameEventListener(GameEvent.OnGameEventListener listener) {
        mListener = listener;
    }

    public void reset() {}

    public List<AppObject> load() { return mObjects; }

    public void release() {
        for (AppObject obj : mObjects) {
            obj.release();
        }
        mObjects.clear();
    }

    public void onSizeChanged(int width, int height) {
        mWidth  = width;
        mHeight = height;

        for (AppObject obj : mObjects) {
            obj.onSizeChanged(width, height);
        }
    }

    public void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLUE);
    }

    // we may get game objects and generate barriers in different thread,
    // so a little synchronization is necessary here
    public List<AppObject> getGameObjects() {
        return mObjects;
    }

    public void updateBarriers() {}

    public void updateReward() {}

    protected void orderByZ(List<AppObject> objects) {
        Collections.sort(objects, new ZOrderComparator());
    }

    protected class ZOrderComparator implements Comparator<AppObject> {
        public int compare(AppObject obj1, AppObject obj2) {
            return obj1.getZOrder() - obj2.getZOrder();
        }
    }

    public void onCollide(AppObject obj, List<AppObject> collideWith) {}
}