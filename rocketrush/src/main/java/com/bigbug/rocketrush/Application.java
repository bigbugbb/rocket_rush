package com.bigbug.rocketrush;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.*;
import android.util.Log;

import java.util.concurrent.Callable;

/**
 * Created by bigbug on 3/5/14.
 */
public class Application extends android.app.Application {

    /**
     * Messages for updating the data in the game
     */
    public static final int MESSAGE_UPDATE_DATA = 1;

    public static final int MESSAGE_START_UPDATING = 2;

    public static final int MESSAGE_STOP_UPDATING = 3;

    /**
     * Message to draw the graph for the game
     */
    public static final int MESSAGE_DRAW_GRAPH = 16;

    public static final int MESSAGE_START_DRAWING = 17;

    public static final int MESSAGE_STOP_DRAWING = 18;

    /**
     * Return value from Callable<Integer> object
     */
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_IGNORE  = 1;
    public static final int RESULT_CANCEL  = 2;

    /**
     * Singleton instance of Application
     */
    private static Application sApplication;

    /**
     * Drawer thread and Updater thread.
     */
    private static HandlerThread sDrawer  = getHandlerThread("Drawer");
    private static HandlerThread sUpdater = getHandlerThread("Updater");

    /**
     * Handlers bound to the drawer thread and updater thread
     */
    private Handler mDrawerHandler;
    private Handler mUpdateHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        sApplication = this;

        mDrawerHandler = new Handler(sDrawer.getLooper()) {

            private boolean mDrawing = false;

            @Override
            public void handleMessage(final Message msg) {

                try {
                    super.handleMessage(msg);

                    if (Globals.IS_LOGGABLE) {
                        Log.v(Globals.LOG_TAG, String.format("Handler received %s", msg)); //$NON-NLS-1$
                    }

                    switch (msg.what) {
                    case MESSAGE_DRAW_GRAPH:
                        if (mDrawing) {
                            Callable<Integer> callable = (Callable<Integer>) msg.obj;
                            if (callable != null) {
                                int result = callable.call();

                                if (result == RESULT_SUCCESS) {
                                    sendMessage(obtainMessage(Application.MESSAGE_DRAW_GRAPH, msg.arg1, msg.arg2, msg.obj));
                                } else if (result == RESULT_IGNORE) {
                                    sendMessage(obtainMessage(Application.MESSAGE_DRAW_GRAPH, msg.arg1, msg.arg2, msg.obj));
                                } else if (result == RESULT_CANCEL) {
                                    // Stop sending message
                                }
                            }
                        }
                        break;
                    case MESSAGE_START_DRAWING:
                        mDrawing = true;
                        break;
                    case MESSAGE_STOP_DRAWING:
                        mDrawing = false;
                        break;
                    default:
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("Fell through switch statement"); //$NON-NLS-1$
                    }
                } catch (final Exception e) {
                    if (Globals.IS_LOGGABLE) {
                        Log.e(Globals.LOG_TAG, "UpdateHandler threw an uncaught exception", e); //$NON-NLS-1$
                    }

                    if (!Globals.IS_EXCEPTION_SUPPRESSION_ENABLED) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        mUpdateHandler = new Handler(sUpdater.getLooper()) {

            private boolean mUpdating = false;

            @Override
            public void handleMessage(final Message msg) {

                try {
                    super.handleMessage(msg);

                    if (Globals.IS_LOGGABLE) {
                        Log.v(Globals.LOG_TAG, String.format("Handler received %s", msg)); //$NON-NLS-1$
                    }

                    switch (msg.what) {
                    case MESSAGE_UPDATE_DATA:
                        if (mUpdating) {
                            Callable<Integer> callable = (Callable<Integer>) msg.obj;
                            if (callable != null) {
                                int result = callable.call();

                                if (result == RESULT_SUCCESS) {
                                    sendMessage(obtainMessage(Application.MESSAGE_UPDATE_DATA, msg.arg1, msg.arg2, msg.obj));
                                } else if (result == RESULT_IGNORE) {
                                    sendMessage(obtainMessage(Application.MESSAGE_UPDATE_DATA, msg.arg1, msg.arg2, msg.obj));
                                } else if (result == RESULT_CANCEL) {
                                    // Stop sending message
                                }
                            }
                        }
                        break;
                    case MESSAGE_START_UPDATING:
                        mUpdating = true;
                        break;
                    case MESSAGE_STOP_UPDATING:
                        mUpdating = false;
                        break;
                    default:
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("Fell through switch statement"); //$NON-NLS-1$
                    }
                } catch (final Exception e) {
                    if (Globals.IS_LOGGABLE) {
                        Log.e(Globals.LOG_TAG, "DrawerHandler threw an uncaught exception", e); //$NON-NLS-1$
                    }

                    if (!Globals.IS_EXCEPTION_SUPPRESSION_ENABLED) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    public static Context getAppContext() {
        return sApplication.getApplicationContext();
    }

    public static Resources getAppResources() {
        return sApplication.getResources();
    }

    public static AssetManager getAppAssets() {
        return sApplication.getAssets();
    }

    public static Handler getUpdateHandler() {
        return sApplication.mUpdateHandler;
    }

    public static Handler getDrawerHandler() {
        return sApplication.mDrawerHandler;
    }

    private static HandlerThread getHandlerThread(final String name) {

        final HandlerThread thread = new HandlerThread(name, android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);

        thread.start();

        return thread;
    }
}
