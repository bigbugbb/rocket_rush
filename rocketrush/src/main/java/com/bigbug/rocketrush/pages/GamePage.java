package com.bigbug.rocketrush.pages;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.activities.SettingActivity;
import com.bigbug.rocketrush.basic.AppCtrl;
import com.bigbug.rocketrush.basic.AppObject;
import com.bigbug.rocketrush.basic.AppPage;
import com.bigbug.rocketrush.game.CtrlEvent;
import com.bigbug.rocketrush.game.GameEvent;
import com.bigbug.rocketrush.game.GameScene;
import com.bigbug.rocketrush.game.SceneEvent;
import com.bigbug.rocketrush.game.StateEvent;
import com.bigbug.rocketrush.utils.MusicPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


public class GamePage extends AppPage implements SensorEventListener {

    /**
     * Game scene which contains all game elements
     */
    private GameScene mScene;

    /**
     * Listener which triggered when the game status has changed
     */
    private OnGameStatusChangedListener mListener;

    /**
     * Queue to receive and retrieve game control events
     */
    private ConcurrentLinkedQueue<CtrlEvent> mEventQueue;

    /**
     * Game controlls generated from user input or sensors
     */
    private List<AppCtrl> mGameCtrls = new ArrayList<AppCtrl>();

    /**
     * Background music
     */
    private int mMusicIDs[] = { R.raw.bkg_music_2, R.raw.bkg_music_3, R.raw.bkg_music_4 };

    private int mMusicIndex;

    private MusicPlayer mMusicPlayer;

    /**
     * Sound effects
     */
    private int mSoundResIDs[] = { R.raw.get_reward_1, R.raw.get_reward_2, R.raw.hit_alient, R.raw.hit_bird, R.raw.hit_stone_thunder };

    private List<Integer> mSoundIDs = new ArrayList<Integer>();

    private SoundPool mSoundPool;


    public GamePage(Context context) {
        super(context);

        mListener = (OnGameStatusChangedListener) context;
        mMusicIndex = 0;
        mMusicPlayer = new MusicPlayer(context);

        mScene = new GameScene(context);
        mScene.setOnGameEventListener(new GameEvent.OnGameEventListener() {

            @Override
            public void onGameEvent(GameEvent event) {

                final Handler handler = new Handler(Looper.getMainLooper());

                if (event.mEventType == GameEvent.EVENT_STATE) {
                    final StateEvent stateEvent = (StateEvent) event;

                    if (stateEvent.mWhat == StateEvent.STATE_OVER) {
                        mScene.setInteractive(false);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Trigger callback method
                                if (mListener != null) {
                                    final HashMap<String, Object> results = new HashMap<String, Object>();
                                    results.put(Globals.KEY_DISTANCE, stateEvent.mExtra);
                                    mListener.onGameOver(results);
                                }
                            }
                        });
                    }
                } else if (event.mEventType == GameEvent.EVENT_SCENE) {
                    final SceneEvent sceneEvent = (SceneEvent) event;

                    if (sceneEvent.mWhat == SceneEvent.SCENE_MILESTONE) {
                        final int level = mScene.onLevelUp();

                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                // Switch to next background music
                                if (level == 1) {
                                    mMusicIndex = 0;
                                } else if (level == 3 || level == 5) {
                                    ++mMusicIndex;
                                } else {
                                    return;
                                }
                                mMusicPlayer.create(mMusicIDs[mMusicIndex]);
                                mMusicPlayer.play();
                            }

                        });

                    } else if (sceneEvent.mWhat == SceneEvent.SCENE_COLLIDE) {
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                int soundID = 0;

                                switch (sceneEvent.mMsg.what) {
                                case AppObject.PROTECTION:
                                    soundID = mSoundIDs.get(0);
                                    break;
                                case AppObject.TIMEBONUS:
                                    soundID = mSoundIDs.get(1);
                                    break;
                                case AppObject.ALIENT:
                                    soundID = mSoundIDs.get(2);
                                    break;
                                case AppObject.BIRD:
                                    soundID = mSoundIDs.get(3);
                                    break;
                                case AppObject.ASTEROID:
                                    soundID = mSoundIDs.get(4);
                                    break;
                                case AppObject.THUNDER:
                                    soundID = mSoundIDs.get(4);
                                    break;
                                }

                                float volume = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext()).getInt(SettingActivity.KEY_SFX, 40) / 100f;
                                mSoundPool.play(soundID, volume, volume, 10, 0, 1);
                            }

                        });
                    }
                }
            }
        });

        mGameCtrls  = new ArrayList<AppCtrl>(10);
        mEventQueue = new ConcurrentLinkedQueue<CtrlEvent>();
    }

    @Override
    public void create() {
        super.create();

        mScene.load();

        mMusicPlayer.create(mMusicIDs[mMusicIndex]);

        if (mSoundPool == null) {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
            for (int resID : mSoundResIDs) {
                int soundID = mSoundPool.load(mContext, resID, 1);
                mSoundIDs.add(Integer.valueOf(soundID));
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        mScene.release();

        mMusicPlayer.destroy();

        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    @Override
    public void resume() {
        mMusicPlayer.play();
    }

    @Override
    public void pause() {
        mMusicPlayer.pause();
    }

    @Override
    public void reset() {
        mScene.reset();
        mMusicIndex = 0;
        mMusicPlayer.create(mMusicIDs[mMusicIndex]);
    }

    @Override
    public void onUpdate() {

        // Surface has not been created
        if (mWidth == 0 || mHeight == 0) {
            return;
        }

        final List<AppObject> objects = mScene.getGameObjects();

        // Retrieve the control event
        final CtrlEvent event = mEventQueue.poll();

        if (event != null) {
            // Interpret the control event and generate proper game controls
            if (event.mWhat == GameEvent.SENSOR_ACCELEROMETER) {
                if (event.mAccX >= 2) {
                    mGameCtrls.add(new AppCtrl(AppCtrl.MOVE_LEFT));
                } else if (event.mAccX <= -2) {
                    mGameCtrls.add(new AppCtrl(AppCtrl.MOVE_RIGHT));
                }
                if (Math.abs(event.mAccY) >= 13) {
                    mGameCtrls.add(new AppCtrl(AppCtrl.MOVE_VERT));
                }
            }
        }

        // Generate rewards and barriers if condition is satisfied.
        mScene.updateReward();
        mScene.updateBarriers();

        // Perform the operation on the game objects based on the game controls
        for (AppCtrl ctrl : mGameCtrls) {
            for (AppObject obj : objects) {
                obj.operate(ctrl);
            }
        }
        mGameCtrls.clear();

        // Update the scene
        mScene.onUpdate();

        // After update the objects' positions, detect collision between each object
        for (AppObject obj : objects) {
            obj.detectCollision(objects);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        mScene.onDraw(canvas);
    }

    @Override
    public void onSizeChanged(int width, int height) {
        mWidth  = width;
        mHeight = height;
        mScene.onSizeChanged(width, height);
    }

    private float mLastX;
    private float mLastY;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            if (mLastX <= -2 && x > -2 && x < 2) {
                mEventQueue.clear();
            } else if (mLastX >= 2 && x > -2 && x < 2) {
                mEventQueue.clear();
            } else if (Math.abs(x) >= 2 || Math.abs(y) >= 13) {
                if (mEventQueue.size() > 4) {
                    return;
                }
                mEventQueue.add(new CtrlEvent((int) x, (int) y, (int) z));
            }

            mLastX = x;
            mLastY = y;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public MusicPlayer getMusicPlayer() {
        return mMusicPlayer;
    }

    /**
     * OnGameStatusChangedListener
     */
    public interface OnGameStatusChangedListener {
        void onGameOver(final HashMap<String, Object> results);
    }
}
