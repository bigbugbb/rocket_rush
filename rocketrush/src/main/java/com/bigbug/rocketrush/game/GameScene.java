package com.bigbug.rocketrush.game;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Message;
import android.os.Vibrator;

import com.bigbug.rocketrush.basic.AppObject;
import com.bigbug.rocketrush.elements.Alient;
import com.bigbug.rocketrush.elements.Asteroid;
import com.bigbug.rocketrush.elements.BackgroundFar;
import com.bigbug.rocketrush.elements.BackgroundNear;
import com.bigbug.rocketrush.elements.Barrier;
import com.bigbug.rocketrush.elements.Bird;
import com.bigbug.rocketrush.elements.Curtain;
import com.bigbug.rocketrush.elements.Field;
import com.bigbug.rocketrush.elements.Level;
import com.bigbug.rocketrush.elements.LifeBar;
import com.bigbug.rocketrush.elements.Odometer;
import com.bigbug.rocketrush.elements.Rocket;
import com.bigbug.rocketrush.elements.SpeedBar;
import com.bigbug.rocketrush.elements.Thunder;
import com.bigbug.rocketrush.elements.TimeBonus;
import com.bigbug.rocketrush.elements.Timer;
import com.bigbug.rocketrush.elements.TrickyAlient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameScene extends BaseScene {

    /**
     * The leading role
     */
    private Rocket mRocket;

    /**
     * Life bar on the top-left and speed bar on the bottom-left
     */
    private LifeBar  mLifeBar;
    private SpeedBar mSpeedBar;

    /**
     * Dynamic background
     */
    private BackgroundFar  mBackgroundFar;
    private BackgroundNear mBackgroundNear;

    /**
     * Game elements which may generate game events
     */
    private Level    mLevel;
    private Odometer mOdometer;
    private Timer 	 mTimer;
    private Curtain  mCurtain;

    /**
     * The current loop. Each loop has 6 levels.
     */
    private int mCurLoop;

    /**
     * The current level. Game difficulty increases as the level increases.
     */
    private int mCurLevel;

    /**
     * Random object used to generate barriers based on the property assigned to the level.
     */
    private Random mRandom;

    public GameScene(Context context) {
        super(context);

        mCurLoop  = 1;
        mCurLevel = 1;

        mRandom = new Random();
    }

    @Override
    public void reset() {
        mCurLevel = 1;
        mCurLoop  = 1;

        mProbBird    = 95;
        mProbAster   = 195;
        mProbAlient  = 145;
        mProbThunder = 255;

        release();
        load();
    }

    public List<AppObject> load() {

        // Create game elements
        if (mBackgroundFar == null) {
            mBackgroundFar = new BackgroundFar(mContext);
            mObjects.add(mBackgroundFar);
        }
        if (mBackgroundNear == null) {
            mBackgroundNear = new BackgroundNear(mContext);
            mObjects.add(mBackgroundNear);
        }
        if (mSpeedBar == null) {
            mSpeedBar = new SpeedBar(mContext);
            mObjects.add(mSpeedBar);
        }
        if (mRocket == null) {
            mRocket = new Rocket(mContext);
            mRocket.setOnCollideListener(this);
            mObjects.add(mRocket);
        }
        if (mLevel == null) {
            mLevel = new Level(mContext);
            mObjects.add(mLevel);
        }
        if (mOdometer == null) {
            mOdometer = new Odometer(mContext);
            mOdometer.setOdometerUpdateListener(mOdometerListener);
            mObjects.add(mOdometer);
        }
        if (mLifeBar == null) {
            mLifeBar = new LifeBar(mContext);
            mLifeBar.setOnLifeChangedListener(mLifebarListener);
            mObjects.add(mLifeBar);
        }
        if (mTimer == null) {
            mTimer = new Timer(mContext);
            mTimer.setOnTimeUpdateListener(mTimerListener);
            mObjects.add(mTimer);
        }
        if (mCurtain == null) {
            mCurtain = new Curtain(mContext);
            mCurtain.setCurtainEventListener(mCurtainListener);
            mObjects.add(mCurtain);
        }

        if (mWidth > 0 || mHeight > 0) {
            for (AppObject obj : mObjects) {
                obj.onSizeChanged(mWidth, mHeight);
            }
        }

        // Order by Z
        orderByZ(mObjects);

        return mObjects;
    }

    public void release() {
        super.release();

        mBackgroundFar  = null;
        mBackgroundNear = null;
        mSpeedBar = null;
        mRocket   = null;
        mLevel    = null;
        mOdometer = null;
        mLifeBar  = null;
        mTimer    = null;
        mCurtain  = null;
    }

    public void setInteractive(boolean interactive) {
        if (mRocket != null) {
            mRocket.setMovable(interactive);
            mRocket.setCollidable(interactive);
            mRocket.setOnCollideListener(interactive ? this : null);
        }
        if (mOdometer != null) {
            mOdometer.setOdometerUpdateListener(interactive ? mOdometerListener : null);
            mOdometer.setEnable(interactive);
        }
        if (mLifeBar != null) {
            mLifeBar.setOnLifeChangedListener(interactive ? mLifebarListener : null);
            mLifeBar.setEnable(interactive);
        }
        if (mSpeedBar != null) {
            mSpeedBar.setEnable(interactive);
        }
        if (mTimer != null) {
            mTimer.setOnTimeUpdateListener(interactive ? mTimerListener : null);
            mTimer.setEnable(interactive);
        }
    }

    @Override
    public void onDraw(Canvas c) {
        for (AppObject obj : mObjects) {
            obj.onDraw(c);
        }
    }

    public void onUpdate() {
        for (AppObject obj : mObjects) {
            obj.onUpdate();
        }
    }

    @Override
    public void onSizeChanged(int width, int height) {
        for (AppObject obj : mObjects) {
            obj.onSizeChanged(width, height);
        }
    }

    @Override
    public void updateBarriers() {

        // Surface has not been created
        if (mWidth == 0 || mHeight == 0) {
            return;
        }

        // Remove invisible barriers
        List<AppObject> invisibles = null;
        for (AppObject b : mBarriers) {
            float x = b.getX(), y = b.getY();
            if (x < -(mWidth >> 2) || x > (mWidth + (mWidth >> 2)) || y > mHeight) {
                if (invisibles == null) {
                    invisibles = new ArrayList<AppObject>();
                }
                invisibles.add(b);
                b.release();
            }
        }
        if (invisibles != null) {
            mBarriers.removeAll(invisibles);
            mObjects.removeAll(invisibles);
        }

        // Create barriers based on the current game progress
        if (mCurLevel == 1) {
            createBird(mProbBird);
        } else if (mCurLevel == 2) {
            createBird(mProbBird);
            createThunder(mProbThunder);
        } else if (mCurLevel == 3) {
            createBird(mProbBird << 2);
            createThunder(mProbThunder << 1);
            createAsteroid(mProbAster);
        } else if (mCurLevel == 4) {
            createThunder(mProbThunder << 2);
            createAsteroid(mProbAster << 1);
            createAlient(mProbAlient << 1);
        } else if (mCurLevel == 5) {
            createThunder(mProbThunder << 1);
            createAsteroid(mProbAster);
            createAlient(mProbAlient << 1);
        } else if (mCurLevel == 6) {
            createThunder(mProbThunder);
            createAsteroid(mProbAster);
            createAlient(mProbAlient);
        }
    }

    /**
     * Probabilities for creating barriers
     */
    private int mProbBird    = 95;
    private int	mProbAster   = 195;
    private int mProbAlient  = 145;
    private int mProbThunder = 255;

    private void createBird(int probability) {

        // Get the acceleration time
        int accTime = mRocket.getAccTime();

        // Generate flying red chicken
        if (mRandom.nextInt(probability) == 1) {
            boolean right = mRandom.nextBoolean();
            Bird bird = new Bird(mContext, right);
            bird.setX(right ? -bird.getWidth() : mWidth);
            bird.setY(mRandom.nextInt(mHeight - (mHeight >> 1) - (accTime > 0 ? (mHeight >> 2) : 0)));
            bird.initSpeeds(
                (right ? mRandom.nextInt(3) + 5 : -5 - mRandom.nextInt(3)) * mLevel.mSpeedScaleX,
                3f,
                accTime
            );
            bird.onSizeChanged(mWidth, mHeight);
            bird.setOnCollideListener(this);
            mBarriers.add(bird);
            mObjects.add(bird);

            // Order by Z
            orderByZ(mObjects);
        }
    }

    private void createAsteroid(int probability) {
        // get the acceleration time
        int accTime = mRocket.getAccTime();
        // generate asteroid
        int type = mRandom.nextInt(probability);
        if (type == 1) {
            Asteroid asteroid = new Asteroid(mContext);
            asteroid.setX(mRandom.nextInt((int) (mWidth - asteroid.getWidth() + 1)));
            asteroid.setY(0 - asteroid.getHeight());
            asteroid.initSpeeds(0, (mRandom.nextInt(3) + 2) * mLevel.mSpeedScaleY * mDip, accTime);
            asteroid.onSizeChanged(mWidth, mHeight);
            asteroid.setOnCollideListener(this);
            mBarriers.add(asteroid);
            mObjects.add(asteroid);
            // order by Z
            orderByZ(mObjects);
        } else if (type == 2) {
            Asteroid asteroid = new Asteroid(mContext);
            boolean right = mRandom.nextBoolean();
            asteroid.setX(right ? -asteroid.getWidth() : mWidth);
            asteroid.setY(mRandom.nextInt(mHeight >> 3) - (accTime > 0 ? (mHeight >> 3) : 0));
            asteroid.initSpeeds(
                    (right ? mRandom.nextInt(3) + 3 : -3 - mRandom.nextInt(3)) * mLevel.mSpeedScaleX * mDip,
                    (mRandom.nextInt(3) + 2) * mLevel.mSpeedScaleY * mDip,
                    accTime
            );
            asteroid.onSizeChanged(mWidth, mHeight);
            asteroid.setOnCollideListener(this);
            mBarriers.add(asteroid);
            mObjects.add(asteroid);
            // order by Z
            orderByZ(mObjects);
        }
    }

    private void createAlient(int probability) {

        // Get the acceleration time
        int accTime = mRocket.getAccTime();

        // Generate alient
        int type = mRandom.nextInt(probability);

//		if (type == 1) {
//			Alient ali = new Alient(mRes);
//			ali.setX(mRandom.nextInt((int)(mWidth - ali.getWidth() + 1)));
//			ali.setY(0 - ali.getHeight());
//			ali.initSpeeds(0, (mRandom.nextInt(4) + 3) * mLevel.mSpeedScaleY, accTime);
//			ali.onSizeChanged(mWidth, mHeight);
//			ali.setOnCollideListener(this);
//			mBarriers.add(ali);
//			mObjects.add(ali);
//			// order by Z
//			orderByZ(mObjects);
//		} else if (type == 2) {
//			Alient ali = new Alient(mRes);
//			boolean right = mRandom.nextBoolean();
//			ali.setX(right ? -ali.getWidth() : mWidth + ali.getWidth());
//			ali.setY(mRandom.nextInt(mHeight >> 3));
//			ali.initSpeeds(
//				(right ? mRandom.nextInt(3) + 3 : -3 - mRandom.nextInt(3)) * mLevel.mSpeedScaleX,
//				(mRandom.nextInt(4) + 3) * mLevel.mSpeedScaleY,
//				accTime
//			);
//			ali.onSizeChanged(mWidth, mHeight);
//			ali.setOnCollideListener(this);
//			mBarriers.add(ali);
//			mObjects.add(ali);
//			// order by Z
//			orderByZ(mObjects);
//		}
        if (type == 3) {

            int aliType = mRandom.nextInt(2);

            Alient alient = new TrickyAlient(mContext, aliType);

            if (aliType == 0) {
                boolean right = mRandom.nextBoolean();
                alient.setX(right ? -alient.getWidth() : mWidth);
                alient.setY(mRandom.nextInt(mHeight >> 5) - (accTime > 0 ? (mHeight >> 3) : 0));
                alient.initSpeeds(
                        (right ? mRandom.nextInt(6) + 7 : -7 - mRandom.nextInt(6)) * mDip,
                        (mRandom.nextInt(4) + 2) * mDip,
                        accTime
                );
            } else if (aliType == 1) {
                float offset = alient.getWidth();
                alient.setX(offset + mRandom.nextInt((int) (mWidth - offset - offset - offset)));
                alient.setY(-alient.getHeight());
                alient.initSpeeds(
                        6 * mDip,
                        (mRandom.nextInt(5) + 2) * mDip,
                        accTime
                );
            }
            alient.onSizeChanged(mWidth, mHeight);
            alient.setOnCollideListener(this);
            mBarriers.add(alient);
            mObjects.add(alient);

            // Order by Z
            orderByZ(mObjects);
        }
    }

    private void createThunder(int probability) {

        // Generate flying red chicken
        if (mRandom.nextInt(probability) == 1) {

            Thunder thunder = new Thunder(mContext);

            thunder.setX(mRandom.nextInt((int) (mWidth - thunder.getWidth())));
            thunder.setY(-thunder.getHeight());
            thunder.initSpeeds(0, 3f * mDip, 0);
            thunder.onSizeChanged(mWidth, mHeight);
            thunder.setOnCollideListener(this);
            mBarriers.add(thunder);
            mObjects.add(thunder);

            // Order by Z
            orderByZ(mObjects);
        }
    }

    // Probabilities for creating reward
    protected int mProbReward = 1250;

    // Flag to indicate whether the time bonus should be generated
    protected boolean mGenerateTimeBonus = false;

    public void updateReward() {

        if (mRandom.nextInt(mProbReward) == 0) {
            Field field = new Field(mContext);

            field.setX(mRandom.nextInt((int) (mWidth - field.getWidth())));
            field.setY(-field.getHeight());
            field.onSizeChanged(mWidth, mHeight);
            field.setOnCollideListener(this);
            mObjects.add(field);

            // Order by Z
            orderByZ(mObjects);
        }

        if (mGenerateTimeBonus) {
            TimeBonus timeBonus = new TimeBonus(mContext);

            timeBonus.setX(mRandom.nextInt((int) (mWidth - timeBonus.getWidth())));
            timeBonus.setY(-timeBonus.getHeight());
            timeBonus.onSizeChanged(mWidth, mHeight);
            timeBonus.setOnCollideListener(this);
            timeBonus.setOnGotTimeBonusListener(mTimebonusListener);
            mObjects.add(timeBonus);

            // Order by Z
            orderByZ(mObjects);
            mGenerateTimeBonus = false;
        }
    }

    public int onLevelUp() {

        // Level up and update barrier probabilities
        mLevel.levelUp();
        mCurLevel = mLevel.getValue() % 7;

        if (mCurLevel == 0) {
            // The difficulty increases about 30% after each loop
            // algorithm:
            // for speed: ...
            // for complexity: 1 / Math.pow(1.1, 6) * 1.363 / 1.1 ≈ 1 / 1.3
            mLevel.mSpeedScaleX *= 0.875;
            mLevel.mSpeedScaleY *= 0.875;
            mProbBird    *= 1.28;
            mProbAster   *= 1.28;
            mProbAlient  *= 1.28;
            mProbThunder *= 1.2;
            ++mCurLoop;
        }
        mCurLevel = mCurLoop > 1 ? mCurLevel + 1 : mCurLevel;

        mProbBird    /= mLevel.mComplexityScale;
        mProbAster   /= mLevel.mComplexityScale;
        mProbAlient  /= mLevel.mComplexityScale;
        mProbThunder /= (mLevel.mComplexityScale - 0.05);

        // Update the background according to the current level
        if (mCurLevel == 1) {
            mCurtain.close();
        } else if (mCurLevel == 3 || mCurLevel == 5) {
            mBackgroundFar.switchToNext();
            mBackgroundNear.switchToNext();
        }

        // Create a timebonus in the next loop
        mGenerateTimeBonus = true;

        return mCurLevel;
    }

    @Override
    public void onCollide(AppObject obj, List<AppObject> collideWith) {

        // Trigger collide effects for all barriers
        float centerX = obj.getX() + obj.getWidth() * 0.5f;
        float centerY = obj.getY() + obj.getHeight() * 0.5f;

        for (AppObject object : collideWith) {
            if (object instanceof Barrier) {
                Barrier barrier = (Barrier) object;
                if (obj.getKind() == AppObject.ROCKET) {
                    ((Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
                    mLifeBar.lifeChange(-0.334f);
                }
                barrier.triggerCollideEffect(obj.getKind(), centerX, centerY);
            }
            Message msg = new Message();
            msg.what = object.getKind();
            GameEvent event = new SceneEvent(SceneEvent.SCENE_COLLIDE, msg);
            mListener.onGameEvent(event);
        }
        orderByZ(mObjects);
    }

    private Odometer.OnOdometerUpdateListener mOdometerListener = new Odometer.OnOdometerUpdateListener() {
        @Override
        public void onReachTarget(int odometer) {
            mLifeBar.lifeChange(0.01f);
        }

        @Override
        public void onReachMilestone(int odometer) {
            Message msg = new Message();
            msg.what = odometer;
            mListener.onGameEvent(new SceneEvent(SceneEvent.SCENE_MILESTONE, msg));
        }
    };

    private LifeBar.OnLifeChangedListener mLifebarListener = new LifeBar.OnLifeChangedListener() {

        @Override
        public void onLifeChanged(float life) {
            if (life == 0) { // compare a float, not good, modify later if necessary
                GameEvent e = new StateEvent(StateEvent.STATE_OVER, StateEvent.NO_LIFE);
                e.mExtra = Integer.valueOf(mOdometer.getDistance());
                mListener.onGameEvent(e);
            }
        }
    };

    private Timer.OnTimeUpdateListener mTimerListener = new Timer.OnTimeUpdateListener() {

        @Override
        public void onTimeUpdate(int curTime) {
            if (curTime == 0) {
                GameEvent event = new StateEvent(StateEvent.STATE_OVER, StateEvent.NO_TIME);
                event.mExtra = Integer.valueOf(mOdometer.getDistance());
                mListener.onGameEvent(event);
            }
        }
    };

    private TimeBonus.OnGotTimeBonusListener mTimebonusListener = new TimeBonus.OnGotTimeBonusListener() {

        @Override
        public void onGotTimeBonus(int bonus) {
            mTimer.addBonusTime(bonus);
        }
    };

    private Curtain.OnCurtainEventListener mCurtainListener = new Curtain.OnCurtainEventListener() {

        @Override
        public void onCurtainPreClosing() {
            setInteractive(false);
        }

        @Override
        public void onCurtainClosed() {
            mCurtain.setDelay(1000);
            mCurtain.open();
            // switch background
            mBackgroundFar.switchToFirst();
            mBackgroundNear.switchToFirst();
        }

        @Override
        public void onCurtainPreOpening() {

        }

        @Override
        public void onCurtainOpened() {
            setInteractive(true);
        }
    };
}
