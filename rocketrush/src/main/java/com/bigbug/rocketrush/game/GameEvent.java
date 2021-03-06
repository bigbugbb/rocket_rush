package com.bigbug.rocketrush.game;

public class GameEvent {

    public static final int EVENT_STATE   = 0;
    public static final int EVENT_CONTROL = 1;
    public static final int EVENT_SCENE   = 2;

    public static final int SENSOR_ACCELEROMETER = 1;
    public int  mEventType;
    public int  mWhat = 0;
    public long mEventTime;
    public Object mExtra = null;

    public GameEvent(int eventType) {
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

    public interface OnGameEventListener {
        void onGameEvent(GameEvent evt);
    }
}