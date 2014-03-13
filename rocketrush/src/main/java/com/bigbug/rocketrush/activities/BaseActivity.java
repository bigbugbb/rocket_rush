package com.bigbug.rocketrush.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;

import com.bigbug.rocketrush.Application;
import com.localytics.android.LocalyticsAmpSession;

/**
 * Created by jefflopes on 3/12/14.
 */
public class BaseActivity extends FragmentActivity {

    protected LocalyticsAmpSession mAmpSession = Application.getLocalyticsSession();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Instantiate the object
        mAmpSession.open();
        mAmpSession.attach(this);
        mAmpSession.upload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAmpSession.open();
        mAmpSession.attach(this);

        if (Build.VERSION.SDK_INT >= 11) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAmpSession.detach();
        mAmpSession.close();
        mAmpSession.upload();
    }
}
