package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bigbug.rocketrush.Application;
import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.game.GameResult;
import com.bigbug.rocketrush.game.GameResults;
import com.bigbug.rocketrush.pages.HomePage;
import com.bigbug.rocketrush.sdktest.EventSetupActivity;
import com.bigbug.rocketrush.utils.BitmapHelper;
import com.bigbug.rocketrush.views.GraphView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class HomeActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Logcat tag
    private static final String TAG = "HomeActivity";

    public static final String KEY_OPEN_FROM_TUTORIAL = "KEY_OPEN_FROM_TUTORIAL";
    public static final String KEY_BACK_FROM_GAME = "KEY_BACK_FROM_GAME";

    // the view for drawing anything
    private GraphView mGraphView;

    private HomePage mHomePage;

    private Handler mUpdater;

    private Handler mDrawer;

    private TextView mTextWelcome;

    /**
     * The bitmaps for the image button.
     */
    private List<Bitmap> mBitmaps;

    /**
     * Google client to interact with Google API, for emulator we need Android 4.2.2 or greater
     */
    private GoogleApiClient mGoogleApiClient;

    private ConnectionResult mConnectionResult;

    private SignInButton mBtnSignIn;

    private boolean mSignInClicked;

    private boolean mIntentInProgress;

    private static final int RC_SIGN_IN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mBitmaps = BitmapHelper.loadBitmaps(this, new int[]{R.drawable.btn_start, R.drawable.btn_start_press, R.drawable.btn_settings, R.drawable.btn_settings_press, R.drawable.btn_help, R.drawable.btn_help_press, R.drawable.btn_rank, R.drawable.btn_rank_press, R.drawable.btn_about, R.drawable.btn_about_press});

        // get views and set listeners
        setupViews();
        // adjust layouts according to the screen resolution
        adjustLayout();

        mDrawer  = Application.getDrawerHandler();
        mUpdater = Application.getUpdateHandler();

        mHomePage = new HomePage(this);
        mHomePage.create();

        mGraphView.setPage(mHomePage);

        // Initializing google plus api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this).addApi(Plus.API, null)
            .addScope(Plus.SCOPE_PLUS_LOGIN).build();

        // Localytics Amp events
        mAmpSession.tagScreen("Home");
        if (getIntent().getBooleanExtra(KEY_OPEN_FROM_TUTORIAL, false)) {
            Object[] info = Application.getLocalyticsEventInfo("Click 'Start Journey'");
            mAmpSession.tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);
        } else if (getIntent().getBooleanExtra(KEY_BACK_FROM_GAME, false)) {
            Object[] info = Application.getLocalyticsEventInfo("Click 'Back'");
            mAmpSession.tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);
        }
        mAmpSession.upload();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
        mBitmaps = null;

        mHomePage.destroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient.connect();

        mHomePage.start();
    }

    @Override
    public void onStop() {
        super.onStop();

        mHomePage.stop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start drawing background
        mDrawer.sendMessage(mDrawer.obtainMessage(Application.MESSAGE_START_DRAWING));
        mDrawer.sendMessage(mDrawer.obtainMessage(Application.MESSAGE_DRAW_GRAPH, new Callable<Integer>() {

            final Object mLock = new Object();

            public Integer call() {

                Canvas c = null;
                SurfaceHolder holder = null;
                try {
                    holder = mGraphView.getHolder();
                    c = holder.lockCanvas(null);
                    synchronized (holder) {
                        if (c != null) {
                            synchronized (mHomePage) {
                                mHomePage.onDraw(c);
                            }
                        }
                    }
                } finally {
                    if (c != null) {
                        holder.unlockCanvasAndPost(c);
                    }
                }

                synchronized (mLock) {
                    try {
                        mLock.wait(20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return Application.RESULT_SUCCESS;
            }
        }));

        // Start updating data
        mUpdater.sendMessage(mUpdater.obtainMessage(Application.MESSAGE_START_UPDATING));
        mUpdater.sendMessage(mUpdater.obtainMessage(Application.MESSAGE_UPDATE_DATA, new Callable<Integer>() {

            final Object mLock = new Object();

            public Integer call() {

                synchronized (mHomePage) {
                    mHomePage.onUpdate();
                }

                synchronized (mLock) {
                    try {
                        mLock.wait(20);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return Application.RESULT_SUCCESS;
            }
        }));

        findViewById(R.id.text_version).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(HomeActivity.this, EventSetupActivity.class));
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop drawing background
        mDrawer.sendMessage(mDrawer.obtainMessage(Application.MESSAGE_STOP_DRAWING));
        // Stop updating data
        mUpdater.sendMessage(mUpdater.obtainMessage(Application.MESSAGE_STOP_UPDATING));
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(KEY_BACK_FROM_GAME, false)) {
            Object[] info = Application.getLocalyticsEventInfo("Click 'Back'");
            mAmpSession.tagScreen("Home");
            mAmpSession.tagEvent((String) info[0], (Map<String, String>) info[1], (List<String>) info[2]);
            mAmpSession.upload();
        }
        setIntent(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//
//        if (keyCode == KeyEvent.KEYCODE_MENU) {
//            imm.toggleSoftInput(0, 0);
//        }
//
//        if (mPwdSetup.isMatch(keyCode)) {
//            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
//            startActivity(new Intent(this, EventSetupActivity.class));
//        }

        return super.onKeyDown(keyCode, event);
    }

    private void setupViews() {
        mGraphView = (GraphView) findViewById(R.id.view_graph);

        // Start game button
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(1)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(0)));

        ImageButton btnStartGame = (ImageButton) findViewById(R.id.btn_start_game);
        btnStartGame.setImageDrawable(states);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, GameActivity.class));
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_on_left);
            }
        });

        // Setting button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(3)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(2)));
        ImageButton btnSetting = (ImageButton) findViewById(R.id.btn_setting);
        btnSetting.setImageDrawable(states);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingActivity.setCallback(new Runnable() {
                    @Override
                    public void run() {
                        Context context = HomeActivity.this.getApplicationContext();
                        float volume = PreferenceManager.getDefaultSharedPreferences(context).getInt(SettingActivity.KEY_SND, 40) / 100f;
                        mHomePage.getMusicPlayer().setVolume(volume, volume);
                    }
                });
                startActivity(new Intent(HomeActivity.this, SettingActivity.class));
            }
        });

        // Help button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, new BitmapDrawable(getResources(), mBitmaps.get(5)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(4)));
        ImageButton btnHelp = (ImageButton) findViewById(R.id.btn_help);
        btnHelp.setImageDrawable(states);
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, TutorialActivity.class);
                intent.putExtra(TutorialActivity.KEY_OPEN_MANUALLY, true);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_on_right);
            }
        });

        // Rank button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(7)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(6)));
        ImageButton btnRank = (ImageButton) findViewById(R.id.btn_rank);
        btnRank.setImageDrawable(states);
        btnRank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, RankActivity.class);
                intent.putExtra(Globals.KEY_GAME_RESULTS, getGameResults());
                startActivity(intent);
            }
        });

        // About button
        states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new BitmapDrawable(getResources(), mBitmaps.get(9)));
        states.addState(new int[] {}, new BitmapDrawable(getResources(), mBitmaps.get(8)));
        ImageButton btnAbout = (ImageButton) findViewById(R.id.btn_about);
        btnAbout.setImageDrawable(states);
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, AboutActivity.class));
//                HashMap<String, String> map = new HashMap<String, String>();
//                map.put("test-key", "0");
//                mAmpSession.tagEvent("test-multi-app-key", map);
//                mTestSession.tagEvent("test-multi-app-key", map);
            }
        });

        // G+ Sign In Button
        mBtnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGplus();
            }
        });

        mTextWelcome = (TextView) findViewById(R.id.text_welcome);
    }

    private void adjustLayout() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Sign-in into google
     * */
    private void signInWithGplus() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignInClicked = true;
            resolveSignInError();
        }
    }

    /**
     * Sign-out from google
     * */
    private void signOutFromGplus() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            updateUI(false);
        }
    }

    /**
     * Revoking access from google
     * */
    private void revokeGplusAccess() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status arg0) {
                            Log.e(TAG, "User access revoked!");
                            mGoogleApiClient.connect();
                            updateUI(false);
                        }

                    });
        }
    }

    /**
     * Method to resolve any signin errors
     * */
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    protected GameResults getGameResults() {
        GameResults results = new GameResults();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int size = sp.getInt(Globals.KEY_RANK_SIZE, 0);
        for (int i = 0; i < size; ++i) {
            int score   = sp.getInt(Globals.KEY_RANK_SCORE + i, 0);
            String date = sp.getString(Globals.KEY_RANK_TIME + i, "");
            GameResult result = new GameResult(score, date);
            results.add(result);
        }
        Collections.sort(results, Collections.reverseOrder());

        return results;
    }

    /**
     * Fetching user's information name, email, profile pic
     * */
    private void getProfileInformation() {
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String personGooglePlusProfile = currentPerson.getUrl();
                String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                sp.edit().putString(Globals.KEY_USER_NAME, personName);

                mTextWelcome.setText(String.format("Welcome! %s", personName));

//                Log.e(TAG, "Name: " + personName + ", plusProfile: "
//                        + personGooglePlusProfile + ", email: " + email
//                        + ", Image: " + personPhotoUrl);
//
//                txtName.setText(personName);
//                txtEmail.setText(email);
//
//                // by default the profile url gives 50x50 px image only
//                // we can replace the value with whatever dimension we want by
//                // replacing sz=X
//                personPhotoUrl = personPhotoUrl.substring(0,
//                        personPhotoUrl.length() - 2)
//                        + PROFILE_PIC_SIZE;
//
//                new LoadProfileImage(imgProfilePic).execute(personPhotoUrl);

            } else {
                Toast.makeText(getApplicationContext(),
                        "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updating the UI, showing/hiding buttons and profile layout
     * */
    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            mBtnSignIn.setVisibility(View.GONE);
            mTextWelcome.setVisibility(View.VISIBLE);
        } else {
            mBtnSignIn.setVisibility(View.VISIBLE);
            mTextWelcome.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode,
                                    Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mSignInClicked = false;
        Log.d(TAG, "User is connected!");

        // Get user's information
        getProfileInformation();

        // Update the UI after signin
        updateUI(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
        updateUI(false);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this,
                    0).show();
            return;
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = connectionResult;

            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }
    }

    /**
     * Background Async task to load user profile picture from url
     * */
    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView mImage;

        public LoadProfileImage(ImageView bmImage) {
            this.mImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap mIcon = null;
            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
                mIcon = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon;
        }

        protected void onPostExecute(Bitmap result) {
            mImage.setImageBitmap(result);
        }
    }
}
