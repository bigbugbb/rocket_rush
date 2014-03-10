package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.game.GameResult;
import com.bigbug.rocketrush.game.GameResults;
import com.bigbug.rocketrush.pages.HomePage;
import com.bigbug.rocketrush.sdktest.EventSetupActivity;
import com.bigbug.rocketrush.utils.BitmapHelper;
import com.bigbug.rocketrush.utils.PasswordChecker;
import com.bigbug.rocketrush.views.GraphView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class HomeActivity extends FragmentActivity {

    // the view for drawing anything
    private GraphView mGraphView;

    private HomePage mHomePage;

    private Handler mUpdater;

    private Handler mDrawer;

    private PasswordChecker mPwdSetup = new PasswordChecker("setup");

    /**
     * The bitmaps for the image button.
     */
    private List<Bitmap> mBitmaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mBitmaps = BitmapHelper.loadBitmaps(this, new int[]{R.drawable.btn_start, R.drawable.btn_start_press, R.drawable.btn_settings, R.drawable.btn_settings_press, R.drawable.btn_help, R.drawable.btn_help_press, R.drawable.btn_rank, R.drawable.btn_rank_press, R.drawable.btn_about, R.drawable.btn_about_press});

        // get views and set listeners
        setupViews();
        // adjust layouts according to the screen resolution
        adjustLayout();

        mDrawer  = Application.getDrawerHandler();
        mUpdater = Application.getUpdateHandler();

        mHomePage = new HomePage(this);
        mHomePage.create();

        mGraphView.setPage(mHomePage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
        mBitmaps = null;

        mHomePage.destroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        mHomePage.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHomePage.stop();
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

        // Start drawing background
        mDrawer.sendMessage(mDrawer.obtainMessage(Application.MESSAGE_START_DRAWING));
        mDrawer.sendMessage(mDrawer.obtainMessage(Application.MESSAGE_DRAW_GRAPH, new Callable<Integer>() {

            final Object mLock = new Object();

            public Integer call() {

                Canvas c = null;
                SurfaceHolder holder = null;
                try {
                    holder = mGraphView.getHolder();
                    c = holder.lockCanvas(null);
                    synchronized (holder) {
                        if (c != null) {
                            synchronized (mHomePage) {
                                mHomePage.onDraw(c);
                            }
                        }
                    }
                } finally {
                    if (c != null) {
                        holder.unlockCanvasAndPost(c);
                    }
                }

                synchronized (mLock) {
                    try {
                        mLock.wait(20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return Application.RESULT_SUCCESS;
            }
        }));

        // Start updating data
        mUpdater.sendMessage(mUpdater.obtainMessage(Application.MESSAGE_START_UPDATING));
        mUpdater.sendMessage(mUpdater.obtainMessage(Application.MESSAGE_UPDATE_DATA, new Callable<Integer>() {

            final Object mLock = new Object();

            public Integer call() {

                synchronized (mHomePage) {
                    mHomePage.onUpdate();
                }

                synchronized (mLock) {
                    try {
                        mLock.wait(20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return Application.RESULT_SUCCESS;
            }
        }));

        findViewById(R.id.text_version).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(HomeActivity.this, EventSetupActivity.class));
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop drawing background
        mDrawer.sendMessage(mDrawer.obtainMessage(Application.MESSAGE_STOP_DRAWING));
        // Stop updating data
        mUpdater.sendMessage(mUpdater.obtainMessage(Application.MESSAGE_STOP_UPDATING));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            imm.toggleSoftInput(0, 0);
        }

        if (mPwdSetup.isMatch(keyCode)) {
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            startActivity(new Intent(this, EventSetupActivity.class));
        }

        return super.onKeyDown(keyCode, event);
    }

    private void setupViews() {
        mGraphView = (GraphView) findViewById(R.id.view_graph);

        // Start game button
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(1)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(0)));

        ImageButton btnStartGame = (ImageButton) findViewById(R.id.btn_start_game);
        btnStartGame.setImageDrawable(states);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, GameActivity.class));
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_on_left);
            }
        });

        // Setting button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(3)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(2)));
        ImageButton btnSetting = (ImageButton) findViewById(R.id.btn_setting);
        btnSetting.setImageDrawable(states);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingActivity.setCallback(new Runnable() {
                    @Override
                    public void run() {
                        Context context = HomeActivity.this.getApplicationContext();
                        float volume = PreferenceManager.getDefaultSharedPreferences(context).getInt(SettingActivity.KEY_SND, 40) / 100f;
                        mHomePage.getMusicPlayer().setVolume(volume, volume);
                    }
                });
                startActivity(new Intent(HomeActivity.this, SettingActivity.class));
            }
        });

        // Help button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, new BitmapDrawable(getResources(), mBitmaps.get(5)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(4)));
        ImageButton btnHelp = (ImageButton) findViewById(R.id.btn_help);
        btnHelp.setImageDrawable(states);
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, TutorialActivity.class));
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_on_right);
            }
        });

        // Rank button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(7)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(6)));
        ImageButton btnRank = (ImageButton) findViewById(R.id.btn_rank);
        btnRank.setImageDrawable(states);
        btnRank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, RankActivity.class);
                intent.putExtra(Globals.KEY_GAME_RESULTS, getGameResults());
                startActivity(intent);
            }
        });

        // About button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(9)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(8)));
        ImageButton btnAbout = (ImageButton) findViewById(R.id.btn_about);
        btnAbout.setImageDrawable(states);
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, AboutActivity.class));
            }
        });
    }

    private void adjustLayout() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    protected GameResults getGameResults() {
        GameResults results = new GameResults();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int size = sp.getInt(Globals.KEY_RANK_SIZE, 0);
        for (int i = 0; i < size; ++i) {
            int score   = sp.getInt(Globals.KEY_RANK_SCORE + i, 0);
            String date = sp.getString(Globals.KEY_RANK_TIME + i, "");
            GameResult result = new GameResult(score, date);
            results.add(result);
        }
        Collections.sort(results, Collections.reverseOrder());

        return results;
    }
}
