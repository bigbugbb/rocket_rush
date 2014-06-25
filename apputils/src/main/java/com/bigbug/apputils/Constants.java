package com.bigbug.apputils;

/**
 * Created by bigbug on 5/1/14.
 */
public class Constants {

    static {
        CURRENT_API_LEVEL = DatapointHelper.getApiLevel();
    }

    public static final String LOG_TAG = "com.bigbug.apputils";

    public static final int CURRENT_API_LEVEL;

    public static final boolean IS_LOGGABLE = true;

    public static final boolean IS_PARAMETER_CHECKING_ENABLED = true;

    public static final boolean IS_EXCEPTION_SUPPRESSION_ENABLED = false;
}
