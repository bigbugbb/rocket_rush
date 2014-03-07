package com.bigbug.rocketrush.game;

public class CtrlEvent extends GameEvent {

    public int mAccX = 0;
    public int mAccY = 0;
    public int mAccZ = 0;

    public CtrlEvent(int accX, int accY, int accZ) {
        super(GameEvent.EVENT_CONTROL);

        mWhat = SENSOR_ACCELEROMETER;

        mAccX = accX;
        mAccY = accY;
        mAccZ = accZ;
    }
}