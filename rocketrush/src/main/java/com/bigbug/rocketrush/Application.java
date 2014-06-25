package com.bigbug.rocketrush;

import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.bigbug.rocketrush.provider.BackendHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.localytics.android.LocalyticsAmpSession;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
     * Drawer thread, Updater thread and Backend thread
     */
    private static final HandlerThread sDrawer;
    private static final HandlerThread sUpdater;
    private static final HandlerThread sBackend;

    /**
     * Handlers bound to the threads
     */
    private Handler mDrawerHandler;
    private Handler mUpdateHandler;
    private Handler mBackendHandler;

    /**
     * Localytics amp session
     */
    private LocalyticsAmpSession mLocalyticsSession;

    static {
        sDrawer = new HandlerThread("Drawer", android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        sDrawer.start();

        sUpdater = new HandlerThread("Updater", android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        sUpdater.start();

        sBackend = new HandlerThread("Backend", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        sBackend.start();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sApplication = this;

        mBackendHandler = new BackendHandler(getApplicationContext(), sBackend.getLooper());
        mBackendHandler.sendMessage(mBackendHandler.obtainMessage(BackendHandler.MESSAGE_INIT));
        mBackendHandler.sendMessage(mBackendHandler.obtainMessage(BackendHandler.MESSAGE_UPDATE_IDENTITY));

        mDrawerHandler = new Handler(sDrawer.getLooper()) {

            private boolean mDrawing = false;

            @Override
            public void handleMessage(final Message msg) {

                try {
                    super.handleMessage(msg);

                    if (Constants.IS_LOGGABLE) {
                        Log.v(Constants.LOG_TAG, String.format("Handler received %s", msg)); //$NON-NLS-1$
                    }

                    switch (msg.what) {
                    case MESSAGE_DRAW_GRAPH:
                        removeMessages(MESSAGE_DRAW_GRAPH);
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
                    if (Constants.IS_LOGGABLE) {
                        Log.e(Constants.LOG_TAG, "UpdateHandler threw an uncaught exception", e); //$NON-NLS-1$
                    }

                    if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED) {
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

                    if (Constants.IS_LOGGABLE) {
                        Log.v(Constants.LOG_TAG, String.format("Handler received %s", msg)); //$NON-NLS-1$
                    }

                    switch (msg.what) {
                    case MESSAGE_UPDATE_DATA:
                        removeMessages(MESSAGE_UPDATE_DATA);
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
                    if (Constants.IS_LOGGABLE) {
                        Log.e(Constants.LOG_TAG, "DrawerHandler threw an uncaught exception", e); //$NON-NLS-1$
                    }

                    if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        mLocalyticsSession = new LocalyticsAmpSession(getApplicationContext(), Constants.LOCALYTICS_SESSION_KEY);
    }

    public static Handler getBackendHandler() {
        return sApplication.mBackendHandler;
    }

    public static Handler getUpdateHandler() {
        return sApplication.mUpdateHandler;
    }

    public static Handler getDrawerHandler() {
        return sApplication.mDrawerHandler;
    }

    public static LocalyticsAmpSession getLocalyticsSession() {
        return sApplication.mLocalyticsSession;
    }

    public static Object[] getLocalyticsEventInfo(final String eventName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(sApplication.getApplicationContext());
        final String jsonAttributes = sp.getString(eventName + Constants._ATTR_KEY, "");
        final String jsonCustomDimensions = sp.getString(eventName + Constants._CUSTOM_DIMENSION_KEY, "");

        Map<String, String> attributesMap = null;
        if (!TextUtils.isEmpty(jsonAttributes)) {
            List<Pair<String, String>> attributesData = new Gson().fromJson(jsonAttributes, new TypeToken<LinkedList<Pair<String, String>>>(){}.getType());
            // Convert List<Pair<String, String>> into HashMap<String, String>
            for (Pair<String, String> pair : attributesData) {
                if (attributesMap == null) {
                    attributesMap = new HashMap<String, String>();
                }
                attributesMap.put(pair.first, pair.second);
            }
        }

        List<String> customDimensionsData = null;
        if (!TextUtils.isEmpty(jsonCustomDimensions)) {
            customDimensionsData = new Gson().fromJson(jsonCustomDimensions, new TypeToken<LinkedList<String>>(){}.getType());
        }

        return new Object[] { eventName, attributesMap, customDimensionsData };
    }

}
