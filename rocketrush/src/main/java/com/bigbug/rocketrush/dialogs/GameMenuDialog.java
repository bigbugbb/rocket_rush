package com.bigbug.rocketrush.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;

public class GameMenuDialog extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_gamemenu);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed },  new ColorDrawable(Color.argb(255, 175, 215, 255)));
        states.addState(new int[] {}, new ColorDrawable(Color.TRANSPARENT));

        final ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(new MenuOptionAdapter(this, new String[] { "Resume", "Restart", "Exit" }));
        listView.setDivider(new ColorDrawable(Color.WHITE));
        listView.setDividerHeight(1);
        listView.setSelector(states);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    finish();
                } else if (position == 1) {
                    setResult(Globals.RESTART_GAME);
                    finish();
                } else if (position == 2) {
                    setResult(Globals.STOP_GAME);
                    finish();
                }
            }

        });

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    class MenuOptionAdapter extends ArrayAdapter<String> {

        public MenuOptionAdapter(Context context, String[] values) {
            super(context, R.layout.menu_item, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View item = inflater.inflate(R.layout.menu_item, parent, false);

            final float dip = getResources().getDisplayMetrics().density;

            TextView option = (TextView) item.findViewById(R.id.text_option);
            option.setText(getItem(position));
            option.setShadowLayer(dip, dip, dip, getResources().getColor(R.color.dark_gray));
            return item;
        }
    }
}