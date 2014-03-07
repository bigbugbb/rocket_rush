package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.dialogs.GameMenuDialog;
import com.bigbug.rocketrush.dialogs.GameOverDialog;
import com.bigbug.rocketrush.pages.GamePage;
import com.bigbug.rocketrush.views.GraphView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class GameActivity extends FragmentActivity implements GamePage.OnGameStatusChangedListener {

    /**
     * The surface view on which to draw the game elements
     */
    private GraphView mGraphView;

    /**
     * The page object interacts with the game scene
     */
    private GamePage mGamePage;

    /**
     * Handler for data updating
     */
    private Handler mUpdater;

    /**
     * Handler for drawing the graph
     */
    private Handler mDrawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mGamePage = new GamePage(this);

        mDrawer  = Application.getDrawerHandler();
        mUpdater = Application.getUpdateHandler();

        mGraphView = (GraphView) findViewById(R.id.view_graph);
        mGraphView.setPage(mGamePage);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGamePage.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGamePage.stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register accelerometer sensor listener
        final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(mGamePage, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

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
                            synchronized (mGamePage) {
                                mGamePage.onDraw(c);
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
                        mLock.wait(Globals.GRAPH_DRAW_INTERVAL);
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

                synchronized (mGamePage) {
                    mGamePage.onUpdate();
                }

                synchronized (mLock) {
                    try {
                        mLock.wait(Globals.DATA_UPDATE_INTERVAL);
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

        // Unregister accelerometer sensor listener
        final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mGamePage);
    }

    private final static int GAMEOVER_DIALOG_REQUEST = 1;

    @Override
    public void onGameOver(final HashMap<String, Object> results) {

        Intent i = new Intent(GameActivity.this, GameOverDialog.class);
        i.putExtra(Globals.KEY_GAME_RESULTS, results);
        startActivityForResult(i, GAMEOVER_DIALOG_REQUEST);

        int distance = (Integer) results.get(Globals.KEY_DISTANCE);
        recordGameResult(distance, DateFormat.getDateInstance().format(new Date()));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            new GameMenuDialog(new Runnable() {
                @Override
                public void run() {
                    mGamePage.restart();
                }
            }).show(getSupportFragmentManager(), "GameMenuDialog");
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GAMEOVER_DIALOG_REQUEST) {
            if (resultCode == GameOverDialog.RETRY) {
                mGamePage.restart();
            } else if (resultCode == GameOverDialog.BACK) {
                finish();
            }
        }
    }

    private void recordGameResult(int score, String time) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int size = sp.getInt(Globals.KEY_RANK_SIZE, 0);
        sp.edit().putInt(Globals.KEY_RANK_SIZE, size + 1).commit();
        sp.edit().putInt(Globals.KEY_RANK_SCORE + size, score).commit();
        sp.edit().putString(Globals.KEY_RANK_TIME + size, time).commit();
    }
}
