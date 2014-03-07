package com.bigbug.rocketrush.pages;

import android.content.Context;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.activities.SettingActivity;
import com.bigbug.rocketrush.basic.AppObject;
import com.bigbug.rocketrush.basic.AppPage;
import com.bigbug.rocketrush.game.GameEvent;
import com.bigbug.rocketrush.game.GameScene;
import com.bigbug.rocketrush.game.SceneEvent;
import com.bigbug.rocketrush.game.StateEvent;
import com.bigbug.rocketrush.media.BackgroundMusic;

import java.util.ArrayList;
import java.util.List;


public class GamePage extends AppPage {

    /**
     * Game scene which contains all game elements
     */
    private GameScene mScene;

    /**
     * Background music
     */
    private int mMusicIDs[] = { R.raw.game_over, R.raw.bkg_music_2, R.raw.bkg_music_3, R.raw.bkg_music_4 };

    private int mMusicIndex;

    private BackgroundMusic mBackgroundMusic = BackgroundMusic.getInstance();

    /**
     * Sound effects
     */
    private int mSoundResIDs[] = { R.raw.get_reward_1, R.raw.get_reward_2, R.raw.hit_alient, R.raw.hit_bird, R.raw.hit_stone_thunder };

    private List<Integer> mSoundIDs = new ArrayList<Integer>();

    private SoundPool mSoundPool;


    public GamePage(Context context) {
        super(context);

        mMusicIndex = 1;

        mScene = new GameScene(context);
        mScene.setOnGameEventListener(new GameEvent.OnGameEventListener() {

            @Override
            public void onGameEvent(GameEvent event) {

                final Handler handler = new Handler(Looper.getMainLooper());

                if (event.mEventType == GameEvent.EVENT_STATE) {
                    final StateEvent stateEvent = (StateEvent) event;

                    if (stateEvent.mWhat == StateEvent.STATE_OVER) {
                        handler.sendMessage(handler.obtainMessage(StateEvent.STATE_OVER, stateEvent.mExtra));

                        // unregister some listeners
                        mScene.setInteractive(false);
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                mBackgroundMusic.create(mContext, mMusicIDs[0]);
                                mBackgroundMusic.setLooping(false);
                                mBackgroundMusic.play();
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
                                if (level == 1) {
                                    mMusicIndex = 1;
                                } else if (level == 3 || level == 5) {
                                    ++mMusicIndex;
                                }
                                mBackgroundMusic.create(mContext, mMusicIDs[mMusicIndex]);
                                mBackgroundMusic.play();
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

                                float volume = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(SettingActivity.SFX_KEY, 40) / 100f;
                                mSoundPool.play(soundID, volume, volume, 10, 0, 1);
                            }

                        });
                    }
                }
            }
        });
    }

    @Override
    public void start() {
        mScene.load();

        mBackgroundMusic.create(mContext, mMusicIDs[mMusicIndex]);
        mBackgroundMusic.play();

        if (mSoundPool == null) {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
            for (int resID : mSoundResIDs) {
                int soundID = mSoundPool.load(mContext, resID, 1);
                mSoundIDs.add(Integer.valueOf(soundID));
            }
        }

        super.start();
    }

    @Override
    public void stop() {
        mScene.release();

        mBackgroundMusic.pause();
        mBackgroundMusic.stop();

        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }

        super.stop();
    }

    @Override
    public void reset() {
        mScene.reset();
        mMusicIndex = 1;
    }

    @Override
    public void onUpdate() {
        mScene.onUpdate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        mScene.onDraw(canvas);
    }

    @Override
    public void onSizeChanged(int width, int height) {
        mScene.onSizeChanged(width, height);
    }
}
