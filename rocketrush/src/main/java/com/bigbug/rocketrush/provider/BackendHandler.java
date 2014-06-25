package com.bigbug.rocketrush.provider;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.bigbug.apputils.DatapointHelper;
import com.bigbug.rocketrush.Constants;
import com.bigbug.rocketrush.provider.RocketRushProvider.IdentityDbColumns;
import com.bigbug.rocketrush.provider.RocketRushProvider.UsersDbColumns;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bigbug on 5/4/14.
 *
 * Operates with database with the handler queue so synchronization and sequence issues are solved.
 */
public class BackendHandler extends Handler {
    /**
     * Empty handler message to initialize the callback.
     * <p>
     * This message must be sent before any other messages.
     */
    public static final int MESSAGE_INIT = 0;

    /**
     * Handler message to update the device info.
     */
    public static final int MESSAGE_UPDATE_IDENTITY = 1;

    /**
     * Handler message to update the user info.
     * <p>
     * {@link Message#obj} is either null or a {@code Map<String, String>} containing attributes for the user info.
     */
    public static final int MESSAGE_UPDATE_USER = 2;

    /**
     * Handler message to record a game play.
     * <p>
     * {@link Message#obj} is either null or a {@code Map<String, String>} containing attributes for the play details.
     */
    public static final int MESSAGE_RECORD_PLAY = 3;

    /**
     * Handler message to register for gcm registration id.
     */
    public static final int MESSAGE_REGISTER_GCM = 4;

    /**
     * Handler message to sign in to the backend.
     */
    public static final int MESSAGE_SIGN_IN = 5;

    /**
     * Handler message to upload data.
     */
    public static final int MESSAGE_UPLOAD_RECORD = 6;

    /**
     * Application context
     */
    protected final Context mContext;

    /**
     * RocketRush database
     */
    protected RocketRushProvider mProvider;

    /**
     * Google cloud messaging, Play Service Library is required
     */
    protected GoogleCloudMessaging mGCM;

    /**
     * Constructs a new Handler that runs on the given looper.
     *
     * @param context The context used to access resources on behalf of the app. It is recommended to use
     *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining
     *            references to {@code Activity} instances. Cannot be null.
     * @throws IllegalArgumentException if {@code context} is null
     * @throws IllegalArgumentException if {@code key} is null or empty
     */
    public BackendHandler(final Context context, Looper looper)
    {
        super(looper);

        if (Constants.IS_PARAMETER_CHECKING_ENABLED) {
            if (null == context) {
                throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
            }
        }

        mContext = context;
    }

    @Override
    public void handleMessage(final Message msg) {
        super.handleMessage(msg);

        if (Constants.IS_LOGGABLE) {
            Log.v(Constants.LOG_TAG, String.format("Handler received %s", msg)); //$NON-NLS-1$
        }

        switch (msg.what) {
        case MESSAGE_INIT:
            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, "Handler received MESSAGE_INIT"); //$NON-NLS-1$
            }

            mProvider = RocketRushProvider.getInstance(mContext);

            break;
        case MESSAGE_UPDATE_IDENTITY:
            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, "Handler received MESSAGE_UPDATE_IDENTITY"); //$NON-NLS-1$
            }

            mProvider.runBatchTransaction(new Runnable() {
                public void run() {
                    BackendHandler.this.updateIdentity();
                }
            });

            break;
        case MESSAGE_UPDATE_USER:
            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, "Handler received MESSAGE_UPDATE_USER"); //$NON-NLS-1$
            }

            mProvider.runBatchTransaction(new Runnable() {
                public void run() {
                    BackendHandler.this.updateUser((Map<String, String>) msg.obj);
                }
            });

            break;
        case MESSAGE_RECORD_PLAY:

            break;
        case MESSAGE_REGISTER_GCM:
            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, "Handler received MESSAGE_REGISTER_GCM"); //$NON-NLS-1$
            }

            mProvider.runBatchTransaction(new Runnable() {
                public void run() {
                    BackendHandler.this.registerGCM();
                }
            });

            break;
        case MESSAGE_SIGN_IN:
            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, "Handler received MESSAGE_SIGN_IN"); //$NON-NLS-1$
            }

            BackendHandler.this.signInBackend((String) msg.obj);

            break;
        case MESSAGE_UPLOAD_RECORD:
            if (Constants.IS_LOGGABLE) {
                Log.v(Constants.LOG_TAG, "Handler received MESSAGE_UPLOAD_RECORD"); //$NON-NLS-1$
            }

            break;
        }
//        catch (final Exception e)
//        {
//            if (Constants.IS_LOGGABLE)
//            {
//                Log.e(Constants.LOG_TAG, "Localytics library threw an uncaught exception", e); //$NON-NLS-1$
//            }
//
//            if (!Constants.IS_EXCEPTION_SUPPRESSION_ENABLED)
//            {
//                throw new RuntimeException(e);
//            }
//        }
    }

    private void updateIdentity() {
        final TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        Cursor cursor = null;
        try {
            cursor = mProvider.query(IdentityDbColumns.TABLE_NAME, null, null, null, null);

            final ContentValues values = new ContentValues();
            values.put(IdentityDbColumns.APP_VERSION, DatapointHelper.getAppVersion(mContext));
            values.put(IdentityDbColumns.ANDROID_VERSION, Build.VERSION.RELEASE);
            values.put(IdentityDbColumns.DEVICE_MODEL, Build.MODEL);
            values.put(IdentityDbColumns.DEVICE_IMEI, DatapointHelper.getTelephonyDeviceIdOrNull(mContext));
            values.putNull(IdentityDbColumns.DEVICE_WIFI_MAC_HASH);
            values.put(IdentityDbColumns.LOCALE_LANGUAGE, Locale.getDefault().getLanguage());
            values.put(IdentityDbColumns.LOCALE_COUNTRY, Locale.getDefault().getCountry());
            values.put(IdentityDbColumns.DEVICE_COUNTRY, telephonyManager.getSimCountryIso());
            values.put(IdentityDbColumns.NETWORK_CARRIER, telephonyManager.getNetworkOperatorName());
            values.put(IdentityDbColumns.NETWORK_COUNTRY, telephonyManager.getNetworkCountryIso());
            values.put(IdentityDbColumns.NETWORK_TYPE, DatapointHelper.getNetworkType(mContext, telephonyManager));

            if (cursor.moveToFirst()) {
                mProvider.update(IdentityDbColumns.TABLE_NAME, values, null, null);
            } else {
                mProvider.insert(IdentityDbColumns.TABLE_NAME, values);
            }
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
    }

    private void updateUser(final Map<String, String> userInfo) {
        boolean isNewUser = true;

        // Get new data from hash map
        final String name  = userInfo.get(RocketRushProvider.UsersDbColumns.NAME);
        final String email = userInfo.get(RocketRushProvider.UsersDbColumns.EMAIL);
        final String score = userInfo.get(RocketRushProvider.UsersDbColumns.SCORE);
        final String image = userInfo.get(RocketRushProvider.UsersDbColumns.IMAGE_URL);

        // Email is used to identify user and it shouldn't be empty
        if (TextUtils.isEmpty(email)) {
            return;
        }

        // Check whether it's a new user or not
        Cursor cursor = null;
        try {
            cursor = mProvider.query(RocketRushProvider.UsersDbColumns.TABLE_NAME, null, String.format("%s = ?", RocketRushProvider.UsersDbColumns.EMAIL), new String[] { userInfo.get(RocketRushProvider.UsersDbColumns.EMAIL) }, null); //$NON-NLS-1$
            if (cursor.moveToFirst()) {
                isNewUser = !email.equals(cursor.getString(cursor.getColumnIndex(RocketRushProvider.UsersDbColumns.EMAIL)));
            }
        } catch (Exception e) {
            if (Constants.IS_LOGGABLE) {
                Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
            }
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;

            }
        }

        if (isNewUser) {
            final ContentValues values = new ContentValues();
            values.put(RocketRushProvider.UsersDbColumns.NAME, name);
            values.put(RocketRushProvider.UsersDbColumns.EMAIL, email);
            values.put(RocketRushProvider.UsersDbColumns.SCORE, 0);
            values.put(RocketRushProvider.UsersDbColumns.IMAGE_URL, image);
            mProvider.insert(RocketRushProvider.UsersDbColumns.TABLE_NAME, values);
        } else {
            final ContentValues values = new ContentValues();
            if (!TextUtils.isEmpty(name)) values.put(RocketRushProvider.UsersDbColumns.NAME, name);
            if (!TextUtils.isEmpty(score)) values.put(RocketRushProvider.UsersDbColumns.SCORE, Integer.valueOf(score));
            if (!TextUtils.isEmpty(image)) values.put(RocketRushProvider.UsersDbColumns.IMAGE_URL, image);
            mProvider.update(RocketRushProvider.UsersDbColumns.TABLE_NAME, values, String.format("%s = ?", RocketRushProvider.UsersDbColumns.EMAIL), new String[] { email });
        }
    }

    private void registerGCM() {
        String appVersion = "";
        String registrationId = "";
        Cursor cursor = null;

        try {
            cursor = mProvider.query(IdentityDbColumns.TABLE_NAME, null, null, null, null); //$NON-NLS-1$

            if (cursor.moveToFirst()) {
                appVersion = cursor.getString(cursor.getColumnIndexOrThrow(IdentityDbColumns.APP_VERSION));
                registrationId = cursor.getString(cursor.getColumnIndexOrThrow(IdentityDbColumns.REGISTRATION_ID));
            }
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }

        final String currentAppVersion = DatapointHelper.getAppVersion(mContext);

        // Only register if we don't have a registration id or if the app version has changed
        if (TextUtils.isEmpty(registrationId) || !appVersion.equals(currentAppVersion)) {
            if (mGCM == null) {
                mGCM = GoogleCloudMessaging.getInstance(mContext);
            }

            try {
                final String newRegistrationId = mGCM.register(Constants.SENDER_ID);

                if (Constants.IS_LOGGABLE) {
                    Log.v(Constants.LOG_TAG, "Device registered, registration ID=" + newRegistrationId);
                }

                final ContentValues values = new ContentValues();
                values.put(IdentityDbColumns.REGISTRATION_ID, newRegistrationId);
                mProvider.update(IdentityDbColumns.TABLE_NAME, values, null, null);
            } catch (IOException e) {
                if (Constants.IS_LOGGABLE) {
                    Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                }
            }
        }
    }

    private void signInBackend(final String email) {
        // Compose the user information for sign in
        final JSONObject info = new JSONObject();
        Cursor cursor = null;
        try {
            cursor = mProvider.query(UsersDbColumns.TABLE_NAME, null, String.format("%s = ?", UsersDbColumns.EMAIL), new String[] { email }, null); //$NON-NLS-1$
            while (cursor.moveToNext()) {
                try {
                    info.put(UsersDbColumns.NAME, cursor.getString(cursor.getColumnIndexOrThrow(UsersDbColumns.NAME)));
                    info.put(UsersDbColumns.EMAIL, cursor.getString(cursor.getColumnIndexOrThrow(UsersDbColumns.EMAIL)));
                    info.put(UsersDbColumns.IMAGE_URL, cursor.getString(cursor.getColumnIndexOrThrow(UsersDbColumns.IMAGE_URL)));
                } catch (final JSONException e) {
                    if (Constants.IS_LOGGABLE) {
                        Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        if (Constants.IS_LOGGABLE) {
            Log.v(Constants.LOG_TAG, String.format("JSON result is %s", info.toString())); //$NON-NLS-1$
        }

    }
}
