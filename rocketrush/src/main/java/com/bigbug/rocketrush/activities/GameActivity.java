package com.bigbug.rocketrush.activities;

import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.pages.GamePage;
import com.bigbug.rocketrush.views.GraphView;

import java.util.concurrent.Callable;

public class GameActivity extends FragmentActivity {

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
                            mGamePage.onDraw(c);
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

                mGamePage.onUpdate();

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
    }

}
