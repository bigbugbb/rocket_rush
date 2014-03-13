package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.utils.BitmapHelper;
import com.bigbug.rocketrush.views.TutorialProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TutorialActivity extends BaseActivity {

    public static final String KEY_OPEN_MANUALLY = "KEY_OPEN_MANUALLY";

    /**
     * The view pager controller for holding and controlling the tutorial fragments.
     */
    private ViewPager mViewPager;

    /**
     * The tutorial progress dots at the bottom of the tutorial view.
     */
    private TutorialProgress mProgressView;

    /**
     * The image button for ending tutorial and start the game activity.
     */
    private ImageButton mButton;

    /**
     * The bitmaps for the image button.
     */
    private List<Bitmap> mBitmaps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        mBitmaps = BitmapHelper.loadBitmaps(this, new int[] { R.drawable.btn_end_tutorial, R.drawable.btn_end_tutorial_press });

        setupView();

        mAmpSession.tagScreen("Tutorial");
        if (getIntent().getBooleanExtra(KEY_OPEN_MANUALLY, false)) {
            Object[] info = Application.getLocalyticsEventInfo("Click 'Help'");
            mAmpSession.tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);
        }
        mAmpSession.upload();
    }

    private void setupView() {

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
        mViewPager.setCurrentItem(0);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            public void onPageScrollStateChanged(int position) {}

            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            public void onPageSelected(int position)
            {
                if (mProgressView != null) {
                    mProgressView.changeTutorial(position);
                }
                mButton.setVisibility(position < 3 ? View.GONE : View.VISIBLE);
            }

        });

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(1)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(0)));

        mButton = (ImageButton) findViewById(R.id.btn_end_tutorial);
        mButton.setImageDrawable(states);
        mButton.setVisibility(View.GONE);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TutorialActivity.this, HomeActivity.class);
                intent.putExtra(HomeActivity.KEY_OPEN_FROM_TUTORIAL, true);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_on_left);
                finish();
            }
        });

        mProgressView = (TutorialProgress) findViewById(R.id.view_progress);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (Bitmap image : mBitmaps) {
            image.recycle();
        }
        mBitmaps = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 11) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            startActivity(new Intent(TutorialActivity.this, HomeActivity.class));
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_on_left);
            finish();
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Fragment pager adapter which helps to create new fragments.
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {

        private List<TutorialPage> mTutorialPages;

        private final static int PAGE_COUNT = 4;

        public ViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);

            mTutorialPages = new ArrayList<TutorialPage>(4);
            for (int i = 0; i < PAGE_COUNT; ++i) {
                mTutorialPages.add(new TutorialPage(i));
            }
        }

        @Override
        public Fragment getItem(int position) {
            return mTutorialPages.get(position);
        }

        @Override
        public int getCount() {
            return mTutorialPages.size();
        }
    }

    /**
     * Fragment for each tutorial page.
     */
    class TutorialPage extends Fragment {

        private int mPageID;

        private TutorialView mTutorialView;

        public TutorialPage(final int pageID) {
            mPageID  = pageID;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            RelativeLayout rootLayout = new RelativeLayout(getActivity());
            rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

            mTutorialView = new TutorialView(getActivity(), mPageID);
            rootLayout.addView(mTutorialView);

            return rootLayout;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            mTutorialView.onDestroy();
        }

        /**
         * The actual view which occupies the whole fragment area.
         */
        class TutorialView extends View {

            /**
             * There is one tutorial view for each tutorial page, the id is used to identify the view.
             */
            protected int mViewID;

            /**
             * The tutorial image.
             */
            protected Bitmap mImage;

            /**
             * The area in which to draw the tutorial image.
             */
            protected Rect mRect;

            public TutorialView(Context context, final int viewID) {
                super(context);
                mViewID = viewID;

                // Load the tutorial image based on the view id
                if (viewID == 0) {
                    mImage = BitmapHelper.loadBitmaps(context, new int[] { R.drawable.tutorial_1 }).get(0);
                } else if (viewID == 1) {
                    mImage = BitmapHelper.loadBitmaps(context, new int[] { R.drawable.tutorial_2 }).get(0);
                } else if (viewID == 2) {
                    mImage = BitmapHelper.loadBitmaps(context, new int[] { R.drawable.tutorial_3 }).get(0);
                } else if (viewID == 3) {
                    mImage = BitmapHelper.loadBitmaps(context, new int[] { R.drawable.tutorial_end }).get(0);
                }
            }

            public void onDestroy() {
                if (mImage != null) {
                    mImage.recycle();
                    mImage = null;
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                canvas.drawBitmap(mImage, null, mRect, null);
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                mRect = new Rect(0, 0, w, h);
            }
        }
    }
}
