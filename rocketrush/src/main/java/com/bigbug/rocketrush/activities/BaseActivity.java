package com.bigbug.rocketrush.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAmpSession.detach();
        mAmpSession.close();
        mAmpSession.upload();
    }
}
