package com.bigbug.rocketrush.dialogs;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.utils.BitmapHelper;

import java.util.HashMap;
import java.util.List;

public class GameOverDialog extends FragmentActivity {

    public final static int RETRY = RESULT_FIRST_USER + 0;
    public final static int BACK  = RESULT_FIRST_USER + 1;

    private HashMap<String, Object> mResults;

    private TextView mTextView = null;

    private List<Bitmap> mBitmaps;

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
                setResult(RETRY);
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
                setResult(BACK);
                finish();
            }
        });

        mTextView = (TextView) findViewById(R.id.text_distance);
        mTextView.setTextColor(Color.RED);
        mTextView.getPaint().setFakeBoldText(true);
        mTextView.setText(String.valueOf(mResults.get(Globals.KEY_DISTANCE)));

        getWindow().setBackgroundDrawable(new BitmapDrawable(getResources(), mBitmaps.get(4)));
//        getWindow().setBackgroundDrawable(new ColorDrawable(0)); // Set transparent background
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
        mBitmaps = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            setResult(BACK);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}