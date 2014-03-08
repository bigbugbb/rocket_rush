package com.bigbug.rocketrush.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;

import com.bigbug.rocketrush.R;

public class AmpSetupActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ampsetup);

        Gallery gallery = (Gallery) findViewById(R.id.gallery);
        gallery.setSpacing(10);
        gallery.setAdapter(new GalleryImageAdapter(this));

        // clicklistener for Gallery
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(AmpSetupActivity.this, "Your selected position = " + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class GalleryImageAdapter extends BaseAdapter
    {
        private Context mContext;

        private Integer[] mImageIds = {
            R.drawable.alient01,
            R.drawable.asteroid01,
            R.drawable.bird_1,
            R.drawable.btn_end_tutorial,
            R.drawable.btn_start,
            R.drawable.btn_retry,
            R.drawable.btn_back,
            R.drawable.btn_settings,
            R.drawable.btn_help,
            R.drawable.btn_rank,
            R.drawable.btn_about
        };

        public GalleryImageAdapter(Context context)
        {
            mContext = context;
        }

        public int getCount() {
            return mImageIds.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        // Override this method according to your need
        public View getView(int index, View view, ViewGroup viewGroup)
        {
            ImageView imageView = new ImageView(mContext);

            imageView.setImageResource(mImageIds[index]);
            imageView.setLayoutParams(new Gallery.LayoutParams(200, 200));

            imageView.setScaleType(ImageView.ScaleType.FIT_XY);

            return imageView;
        }
    }
}
