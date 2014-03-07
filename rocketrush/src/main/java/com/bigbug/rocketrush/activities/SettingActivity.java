package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.media.BackgroundMusic;

public class SettingActivity extends FragmentActivity implements SeekBar.OnSeekBarChangeListener {

    public final static String SND_KEY = "sound volume";
    public final static String SFX_KEY = "sfx volume";

    protected SeekBar mSndSeek;
    protected SeekBar mSfxSeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        setupViews();
        adjustLayout();
    }

    private void setupViews() {
        Context context = getApplicationContext();

        mSndSeek = (SeekBar) findViewById(R.id.volume_seek);
        mSndSeek.setOnSeekBarChangeListener(this);
        mSndSeek.setProgress(PreferenceManager.getDefaultSharedPreferences(context).getInt(SND_KEY, 40));

        mSfxSeek = (SeekBar) findViewById(R.id.sfx_seek);
        mSfxSeek.setOnSeekBarChangeListener(this);
        mSfxSeek.setProgress(PreferenceManager.getDefaultSharedPreferences(context).getInt(SFX_KEY, 40));
    }

    private void adjustLayout() {
        DisplayMetrics dm = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(dm);

        ViewGroup.LayoutParams laParams = null;
        laParams = mSndSeek.getLayoutParams();
        laParams.width = (int) (dm.widthPixels * 0.5f);
        mSndSeek.setLayoutParams(laParams);
        laParams = mSfxSeek.getLayoutParams();
        laParams.width = (int) (dm.widthPixels * 0.5f);
        mSfxSeek.setLayoutParams(laParams);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

        if (seekBar == mSndSeek) {
            editor.putInt(SND_KEY, progress);
            float volume = progress / 100f;
            BackgroundMusic.getInstance().setVolume(volume, volume);
        } else if (seekBar == mSfxSeek) {
            editor.putInt(SFX_KEY, progress);
        }

        editor.commit();
    }

    public void onStartTrackingTouch(SeekBar seekBar) {}

    public void onStopTrackingTouch(SeekBar seekBar) {}

}