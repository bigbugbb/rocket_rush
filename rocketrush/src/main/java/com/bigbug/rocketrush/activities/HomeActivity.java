package com.bigbug.rocketrush.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.game.GameResult;
import com.bigbug.rocketrush.game.GameResults;
import com.bigbug.rocketrush.pages.HomePage;
import com.bigbug.rocketrush.utils.BitmapHelper;
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
        mGraphView.setPage(mHomePage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
        mBitmaps = null;
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
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop drawing background
        mDrawer.sendMessage(mDrawer.obtainMessage(Application.MESSAGE_STOP_DRAWING));
        // Stop updating data
        mUpdater.sendMessage(mUpdater.obtainMessage(Application.MESSAGE_STOP_UPDATING));
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
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
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
