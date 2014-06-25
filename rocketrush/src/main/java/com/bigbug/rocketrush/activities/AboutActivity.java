package com.bigbug.rocketrush.activities;

import android.os.Bundle;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.R;

import java.util.List;
import java.util.Map;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Object[] info = Application.getLocalyticsEventInfo("Click 'About'");
        mSession.tagScreen("About");
        mSession.tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);
        mSession.upload();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
