package com.bigbug.rocketrush.media;

import android.content.Context;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;

import com.bigbug.rocketrush.activities.SettingActivity;

public class BackgroundMusic {

    private int mSeekPos;

    private MediaPlayer mPlayer;

    private static BackgroundMusic sMusic = new BackgroundMusic();

    public static BackgroundMusic getInstance() {
        return sMusic;
    }

    private BackgroundMusic() {
        mPlayer = new MediaPlayer();
        mSeekPos = 0;
    }

    public void create(Context context, int resource) {
        stop();
        // Start music only if not disabled in preferences
        mPlayer = MediaPlayer.create(context, resource);
        if (mPlayer != null) {
            mPlayer.setLooping(true);
            float volume =
                    PreferenceManager.getDefaultSharedPreferences(context).getInt(SettingActivity.SND_KEY, 40) / 100f;
            mPlayer.setVolume(volume, volume);
        }
        mSeekPos = 0;
    }

    public void setLooping(boolean looping) {
        if (mPlayer != null) {
            mPlayer.setLooping(looping);
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (mPlayer != null) {
            mPlayer.setVolume(leftVolume, rightVolume);
        }
    }

    public void play() {
        if (mPlayer != null) {
            if (!mPlayer.isPlaying()) {
                mPlayer.seekTo(mSeekPos);
                mPlayer.start();
            }
        }
    }

    public void reset() {
        mSeekPos = 0;
    }

    public void pause() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                mSeekPos = mPlayer.getCurrentPosition();
            }
        }
    }

    /** Stop the music */
    public void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
            System.gc();
        }
    }
}