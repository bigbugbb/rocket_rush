package com.bigbug.rocketrush.dialogs;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.utils.BitmapHelper;
import com.bigbug.rocketrush.utils.MusicPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameOverDialog extends FragmentActivity {

    private HashMap<String, Object> mResults;

    private TextView mTextView = null;

    private List<Bitmap> mBitmaps;

    private MusicPlayer mMusicPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_gameover);

        mBitmaps = BitmapHelper.loadBitmaps(this, new int[] { R.drawable.btn_retry, R.drawable.btn_retry_press, R.drawable.btn_back, R.drawable.btn_back_press, R.drawable.bg_gameover });

        mResults = (HashMap<String, Object>) getIntent().getExtras().get(Globals.KEY_GAME_RESULTS);

        // Start buttons
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(1)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(0)));
        ImageButton btnRetry = (ImageButton) findViewById(R.id.btn_retry);
        btnRetry.setImageDrawable(states);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object[] info = Application.getLocalyticsEventInfo("Click 'Retry'");
                Application.getLocalyticsSession().tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);

                setResult(Globals.RESTART_GAME);
                finish();
            }
        });

        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(3)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(2)));
        ImageButton btnBack = (ImageButton) findViewById(R.id.btn_back);
        btnBack.setImageDrawable(states);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object[] info = Application.getLocalyticsEventInfo("Click 'Back'");
                Application.getLocalyticsSession().tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);

                setResult(Globals.STOP_GAME);
                finish();
            }
        });

        mTextView = (TextView) findViewById(R.id.text_distance);
        mTextView.setTextColor(Color.RED);
        mTextView.getPaint().setFakeBoldText(true);
        mTextView.setText(String.valueOf(mResults.get(Globals.KEY_DISTANCE)));

        getWindow().setBackgroundDrawable(new BitmapDrawable(getResources(), mBitmaps.get(4)));
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

        mMusicPlayer = new MusicPlayer(this);
        mMusicPlayer.create(R.raw.game_over);
        mMusicPlayer.setLooping(false);
        mMusicPlayer.play();

        // Instantiate the object
        Application.getLocalyticsSession().open();
        Application.getLocalyticsSession().attach(this);
        Application.getLocalyticsSession().tagScreen("Game Over");
        Application.getLocalyticsSession().upload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mMusicPlayer.destroy();

        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
        mBitmaps = null;
    }

    @Override
    protected void onPause() {
        Application.getLocalyticsSession().detach();
        Application.getLocalyticsSession().close();
        Application.getLocalyticsSession().upload();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Application.getLocalyticsSession().open();
        Application.getLocalyticsSession().attach(this);
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}