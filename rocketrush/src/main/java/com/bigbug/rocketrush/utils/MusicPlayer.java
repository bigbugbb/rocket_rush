package com.bigbug.rocketrush.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;

import com.bigbug.rocketrush.activities.SettingActivity;

public class MusicPlayer {

    private MediaPlayer mPlayer;

    private Context mContext;

    public MusicPlayer(Context context) {
        mPlayer  = null;
        mContext = context.getApplicationContext();
    }

    public void create(int resource) {
        if (isPlaying()) {
            stop();
        }
        mPlayer = MediaPlayer.create(mContext, resource);
        if (mPlayer != null) {
            mPlayer.setLooping(true);
            float volume = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(SettingActivity.KEY_SND, 40) / 100f;
            mPlayer.setVolume(volume, volume);
        }
    }

    public void setLooping(boolean looping) {
        mPlayer.setLooping(looping);
    }

    public void setVolume(float leftVolume, float rightVolume) {
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    public void play() {
        mPlayer.start();
    }

    public void reset() {
        mPlayer.seekTo(0);
    }

    public void pause() {
        mPlayer.pause();
    }

    public void stop() {
        mPlayer.stop();
    }

    /** Stop the music */
    public void destroy() {
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
    }

    public boolean isPlaying() {
        if (mPlayer == null) {
            return false;
        }
        return mPlayer.isPlaying();
    }
}
