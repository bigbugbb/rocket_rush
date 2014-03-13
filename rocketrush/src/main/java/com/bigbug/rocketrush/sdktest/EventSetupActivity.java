package com.bigbug.rocketrush.sdktest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bigbug.rocketrush.R;
import com.bigbug.rocketrush.utils.Dynamics;
import com.bigbug.rocketrush.utils.MyListView;

import java.util.ArrayList;

public class EventSetupActivity extends FragmentActivity {

    /** Id for the toggle rotation menu item */
    private static final int TOGGLE_ROTATION_MENU_ITEM = 0;

    /** Id for the toggle lighting menu item */
    private static final int TOGGLE_LIGHTING_MENU_ITEM = 1;

    /** The list view */
    private MyListView mListView;

    private final static String KEY_EVENT_NAME = "KEY_EVENT_NAME_";

    /** The image resource id for all events */
    private int[] mImages = {
        R.drawable.alient01,
        R.drawable.asteroid01,
        R.drawable.bird_1,
        R.drawable.thunder,
        R.drawable.single_protector_1,
        R.drawable.single_time_bonus_1,
        R.drawable.btn_end_tutorial,
        R.drawable.btn_start,
        R.drawable.btn_retry,
        R.drawable.btn_back,
        R.drawable.btn_settings_press,
        R.drawable.btn_help_press,
        R.drawable.btn_rank_press,
        R.drawable.btn_about_press
    };

    /** The default event names corresponding to the image resource id */
    private String[] mNames = {
        "Hit Alient",
        "Hit Asteroid",
        "Hit Bird",
        "Hit Thunder",
        "Get Protector",
        "Get Time Bonus",
        "Click 'Start Journey'",
        "Click 'Play'",
        "Click 'Retry/Restart'",
        "Click 'Back'",
        "Click 'Setting'",
        "Click 'Help'",
        "Click 'Rank'",
        "Click 'About'"
    };

    /**
     * Small class that represents a contact
     */
    private static class Event {

        /** Event image id */
        int mImage;

        /** Name of the event */
        String mName;

        /**
         * Constructor
         *
         * @param image The image resource id
         * @param name The event name
         */
        public Event(final int image, final String name) {
            mImage = image;
            mName  = name;
        }

    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ampsetup);

        final ArrayList<Event> events = createEventList();
        final EventListAdapter adapter = new EventListAdapter(this, events);

        mListView = (MyListView)findViewById(R.id.list_events);
        mListView.setAdapter(adapter);

        mListView.setDynamics(new SimpleDynamics(0.9f, 0.6f));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {
                final String message = "OnClick: " + events.get(position).mName;
                Intent intent = new Intent(EventSetupActivity.this, EventSetupDialog.class);
                intent.putExtra(EventSetupDialog.KEY_EVENT_NAME, mNames[position]);
                startActivity(intent);
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                           final int position, final long id) {
                return true;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.add(Menu.NONE, TOGGLE_ROTATION_MENU_ITEM, 0, "Toggle Rotation");
        menu.add(Menu.NONE, TOGGLE_LIGHTING_MENU_ITEM, 1, "Toggle Lighting");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case TOGGLE_ROTATION_MENU_ITEM:
                mListView.enableRotation(!mListView.isRotationEnabled());
                return true;

            case TOGGLE_LIGHTING_MENU_ITEM:
                mListView.enableLight(!mListView.isLightEnabled());
                return true;

            default:
                return false;
        }
    }

    /**
     * Creates a list of events
     *
     * @return A list of events
     */
    private ArrayList<Event> createEventList() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final ArrayList<Event> events = new ArrayList<Event>();
        for (int i = 0; i < mImages.length; ++i) {
            events.add(new Event(mImages[i], sp.getString(KEY_EVENT_NAME + i, mNames[i])));
        }
        return events;
    }

    /**
     * Adapter class to use for the list
     */
    private static class EventListAdapter extends ArrayAdapter<Event> {

        /**
         * Constructor
         *
         * @param context The context
         * @param contacts The list of contacts
         */
        public EventListAdapter(final Context context, final ArrayList<Event> contacts) {
            super(context, 0, contacts);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.event_item, null);
            }

            final TextView event = (TextView) view.findViewById(R.id.text_event);
            event.setText(getItem(position).mName);

            final ImageView target = (ImageView)view.findViewById(R.id.image_target);
            target.setImageResource(getItem(position).mImage);

            return view;
        }
    }

    /**
     * A very simple dynamics implementation with spring-like behavior
     */
    class SimpleDynamics extends Dynamics {

        /** The friction factor */
        private float mFrictionFactor;

        /** The snap to factor */
        private float mSnapToFactor;

        /**
         * Creates a SimpleDynamics object
         *
         * @param frictionFactor The friction factor. Should be between 0 and 1.
         *            A higher number means a slower dissipating speed.
         * @param snapToFactor The snap to factor. Should be between 0 and 1. A
         *            higher number means a stronger snap.
         */
        public SimpleDynamics(final float frictionFactor, final float snapToFactor) {
            mFrictionFactor = frictionFactor;
            mSnapToFactor = snapToFactor;
        }

        @Override
        protected void onUpdate(final int dt) {
            // update the velocity based on how far we are from the snap point
            mVelocity += getDistanceToLimit() * mSnapToFactor;

            // then update the position based on the current velocity
            mPosition += mVelocity * dt / 1000;

            // and finally, apply some friction to slow it down
            mVelocity *= mFrictionFactor;
        }
    }

//    public class GalleryImageAdapter extends BaseAdapter
//    {
//        private Context mContext;
//
//        private Integer[] mImageIds = {
//                R.drawable.alient01,
//                R.drawable.asteroid01,
//                R.drawable.bird_1,
//                R.drawable.btn_end_tutorial,
//                R.drawable.btn_start,
//                R.drawable.btn_retry,
//                R.drawable.btn_back,
//                R.drawable.btn_settings,
//                R.drawable.btn_help,
//                R.drawable.btn_rank,
//                R.drawable.btn_about
//        };
//
//        public GalleryImageAdapter(Context context)
//        {
//            mContext = context;
//        }
//
//        public int getCount() {
//            return mImageIds.length;
//        }
//
//        public Object getItem(int position) {
//            return position;
//        }
//
//        public long getItemId(int position) {
//            return position;
//        }
//
//        // Override this method according to your need
//        public View getView(int index, View view, ViewGroup viewGroup)
//        {
//            ImageView imageView = new ImageView(mContext);
//
//            imageView.setImageResource(mImageIds[index]);
//            imageView.setLayoutParams(new Gallery.LayoutParams(200, 200));
//
//            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
//
//            return imageView;
//        }
//    }

//    public class GalleryImageAdapter extends BaseAdapter
//    {
//        private Context mContext;
//
//        private Integer[] mImageIds = {
//            R.drawable.alient01,
//            R.drawable.asteroid01,
//            R.drawable.bird_1,
//            R.drawable.btn_end_tutorial,
//            R.drawable.btn_start,
//            R.drawable.btn_retry,
//            R.drawable.btn_back,
//            R.drawable.btn_settings,
//            R.drawable.btn_help,
//            R.drawable.btn_rank,
//            R.drawable.btn_about
//        };
//
//        public GalleryImageAdapter(Context context)
//        {
//            mContext = context;
//        }
//
//        public int getCount() {
//            return mImageIds.length;
//        }
//
//        public Object getItem(int position) {
//            return position;
//        }
//
//        public long getItemId(int position) {
//            return position;
//        }
//
//        // Override this method according to your need
//        public View getView(int index, View view, ViewGroup viewGroup)
//        {
//            ImageView imageView = new ImageView(mContext);
//
//            imageView.setImageResource(mImageIds[index]);
//            imageView.setLayoutParams(new Gallery.LayoutParams(200, 200));
//
//            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
//
//            return imageView;
//        }
//    }
}
