package com.bigbug.rocketrush.basic;

public class AppEvent {
    public static final int EVENT_STATE   = 0;
    public static final int EVENT_CONTROL = 1;
    public static final int EVENT_SCENE   = 2;
    public static final int SENSOR_ACCELEROMETER = 1;

    public int    mEventType;
    public int    mWhat;
    public long   mEventTime;
    public Object mExtra;

    public AppEvent(int eventType) {
        setEventType(eventType);
        mEventTime = System.currentTimeMillis();
    }

    public void setEventType(int eventType) {
        mEventType = eventType;
    }

    public int getEventType() {
        return mEventType;
    }

    public long getEventTime() {
        return mEventTime;
    }
}