package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.R;

import java.util.List;
import java.util.Map;

public class SettingActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    public final static String KEY_SND = "KEY_SND";
    public final static String KEY_SFX = "KEY_SFX";

    private SeekBar mSndSeek;
    private SeekBar mSfxSeek;

    private static Runnable sCallback;

    static public void setCallback(final Runnable callback) {
        sCallback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        setupViews();
        adjustLayout();

        Object[] info = Application.getLocalyticsEventInfo("Click 'Setting'");
        mAmpSession.tagScreen("Setting");
        mAmpSession.tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);
        mAmpSession.upload();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 11) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void setupViews() {
        Context context = getApplicationContext();

        mSndSeek = (SeekBar) findViewById(R.id.volume_seek);
        mSndSeek.setOnSeekBarChangeListener(this);
        mSndSeek.setProgress(PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_SND, 40));

        mSfxSeek = (SeekBar) findViewById(R.id.sfx_seek);
        mSfxSeek.setOnSeekBarChangeListener(this);
        mSfxSeek.setProgress(PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_SFX, 40));
    }

    private void adjustLayout() {
        Display display = getWindowManager().getDefaultDisplay();

        ViewGroup.LayoutParams laParams = null;
        laParams = mSndSeek.getLayoutParams();
        laParams.width = (int) (display.getWidth() * 0.5f);
        mSndSeek.setLayoutParams(laParams);
        laParams = mSfxSeek.getLayoutParams();
        laParams.width = (int) (display.getWidth() * 0.5f);
        mSfxSeek.setLayoutParams(laParams);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

        if (seekBar == mSndSeek) {
            editor.putInt(KEY_SND, progress);
        } else if (seekBar == mSfxSeek) {
            editor.putInt(KEY_SFX, progress);
        }

        editor.commit();

        if (sCallback != null) {
            sCallback.run();
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {}

    public void onStopTrackingTouch(SeekBar seekBar) {}

}