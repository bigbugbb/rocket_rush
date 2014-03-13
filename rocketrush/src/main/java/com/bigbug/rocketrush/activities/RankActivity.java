package com.bigbug.rocketrush.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.game.GameResult;

import java.util.List;
import java.util.Map;

public class RankActivity extends BaseActivity {

    protected TableLayout mTable;
    protected TextView mTextRecord;
    protected List<GameResult> mResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        mResults = (List<GameResult>) getIntent().getSerializableExtra(Globals.KEY_GAME_RESULTS);

        // get views and set listeners
        setupViews();
        // adjust layouts according to the screen resolution
        adjustLayout();

        Object[] info = Application.getLocalyticsEventInfo("Click 'Rank'");
        mAmpSession.tagScreen("Rank");
        mAmpSession.tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);
        mAmpSession.upload();
    }

    private void setupViews() {
        mTable      = (TableLayout) findViewById(R.id.tableLayoutRocketRank);
        mTextRecord = (TextView) findViewById(R.id.rankNoRecordTextView);
    }

    private void adjustLayout() {

        // get screen resolution
        DisplayMetrics dm = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(dm);
        // adjust the layout according to the screen resolution
        ViewGroup.LayoutParams laParams = null;
        ScrollView scrollView = (ScrollView)findViewById(R.id.rocketrankScrollView);
        laParams = scrollView.getLayoutParams();
        laParams.height = (int) (dm.heightPixels * 0.7f);
        scrollView.setLayoutParams(laParams);
        getWindow().setLayout((int)(dm.widthPixels * 0.9f), (int)(dm.heightPixels * 0.9f));
    }

    @Override
    protected void onResume() {
        if (mResults == null || mResults.size() == 0) {
            mTextRecord.setVisibility(View.VISIBLE);
        } else {
            mTextRecord.setVisibility(View.GONE);
            for (int i = 0; i < mResults.size(); ++i) {
                TableRow tablerow = new TableRow(getApplicationContext());
                tablerow.setBackgroundColor(Color.rgb(255, 255, 255));

                TextView textViewRank = new TextView(getApplicationContext());
                textViewRank.setTextSize(16);
                textViewRank.setTextColor(getApplicationContext().getResources().getColor(R.color.gray));
                textViewRank.setPadding(10, 10, 10, 10);
                textViewRank.setWidth(10);
                textViewRank.setText(String.valueOf(i + 1));
                textViewRank.getPaint().setFakeBoldText(true);
                tablerow.addView(textViewRank);

                TextView textViewScore = new TextView(getApplicationContext());
                textViewScore.setTextSize(16);
                textViewScore.setTextColor(getApplicationContext().getResources().getColor(R.color.gray));
                textViewScore.setPadding(10, 10, 10, 10);
                textViewScore.setWidth(50);
                textViewScore.getPaint().setFakeBoldText(true);
                textViewScore.setText(String.valueOf(mResults.get(i).mScore));
                tablerow.addView(textViewScore);

                TextView textViewTime = new TextView(getApplicationContext());
                textViewTime.setTextSize(16);
                textViewTime.setTextColor(getApplicationContext().getResources().getColor(R.color.gray));
                textViewTime.setPadding(10, 10, 10, 10);
                textViewTime.setWidth(70);
                textViewTime.getPaint().setFakeBoldText(true);
                textViewTime.setText(mResults.get(i).getDate());
                tablerow.addView(textViewTime);

                mTable.addView(tablerow, new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }

        super.onResume();
    }

}