package com.bigbug.rocketrush;

import android.app.Activity;

/**
 * Created by jefflopes on 3/5/14.
 */
public class Globals {

    public static final String LOG_TAG = "com.bigbug.rocketrush";

    public static final boolean IS_LOGGABLE = false;

    public static final boolean IS_PARAMETER_CHECKING_ENABLED = false;

    public static final boolean IS_EXCEPTION_SUPPRESSION_ENABLED = false;

    public static final int DATA_UPDATE_INTERVAL = 20;

    public static final int GRAPH_DRAW_INTERVAL = 10;

    public static final String KEY_USER_NAME = "KEY_USER_NAME";

    public static final String KEY_GAME_RESULTS = "KEY_GAME_RESULTS";

    public static final String KEY_DISTANCE = "KEY_DISTANCE";

    public final static String KEY_RANK_SIZE  = "KEY_RANK_SIZE_";
    public final static String KEY_RANK_SCORE = "KEY_RANK_SCORE_";
    public final static String KEY_RANK_TIME  = "KEY_RANK_TIME_";

    public final static String KEY_CALLBACK = "KEY_CALLBACK";

    public final static String KEY_FIRST_GAME = "KEY_FIRST_GAME";

    public final static int RESTART_GAME = Activity.RESULT_FIRST_USER + 1;
    public final static int STOP_GAME    = Activity.RESULT_FIRST_USER + 2;

    public final static int GAME_TIME = 40;

    public final static String _ATTR_KEY   = "_ATTR_KEY";
    public final static String _CUSTOM_DIMENSION_KEY = "_CUSTOM_KEY";

    public final static String LOCALYTICS_SESSION_KEY = "f0d4b5fb7aadc3caefaff91-626d9720-aeb2-11e3-d642-0056aeeaa726";
}
