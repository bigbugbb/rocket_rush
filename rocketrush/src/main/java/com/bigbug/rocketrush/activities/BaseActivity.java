package com.bigbug.rocketrush.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.WindowManager;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Constants;
import com.bigbug.rocketrush.provider.BackendHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.localytics.android.LocalyticsAmpSession;

/**
 * Created by jefflopes on 3/12/14.
 */
public class BaseActivity extends FragmentActivity {

    protected LocalyticsAmpSession mSession = Application.getLocalyticsSession();

    protected Handler mBackendHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Instantiate the object
        mSession.open();
        mSession.attach(this);
        mSession.upload();

        // Get backend handler
        mBackendHandler = Application.getBackendHandler();

        // Check device for Play Services APK. If check succeeds, proceed with gcm registration.
        if (checkPlayServices()) {
            mBackendHandler.sendMessage(mBackendHandler.obtainMessage(BackendHandler.MESSAGE_REGISTER_GCM, null));
        } else {
            Log.i(Constants.LOG_TAG, "No valid Google Play Services APK found.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSession.open();
        mSession.attach(this);

        // Check device for Play Services APK.
        checkPlayServices();

//        if (Build.VERSION.SDK_INT >= 11) {
//            getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
//                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
//                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
//                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
//                View.SYSTEM_UI_FLAG_FULLSCREEN |
//                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//            );
//        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSession.detach();
        mSession.close();
        mSession.upload();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(Constants.LOG_TAG, "This device is not supported.");
            }
            return false;
        }
        return true;
    }
}
