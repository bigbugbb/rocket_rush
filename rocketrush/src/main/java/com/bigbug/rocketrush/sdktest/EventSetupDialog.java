package com.bigbug.rocketrush.sdktest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.bigbug.rocketrush.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.LinkedList;

public class EventSetupDialog extends FragmentActivity {

    public static final String KEY_EVENT_NAME = "KEY_EVENT_NAME";

    private ViewGroup mLayoutDialog;

    private ListView mListAttr;

    private ListView mListCustom;

    private AttributesAdapter mAttributesAdapter;

    private CustomDimensionsAdapter mCustomDimensionsAdapter;

    private SharedPreferences mSharedPref;

    private String mEventName;

    private final static String _ATTR_KEY   = "_ATTR_KEY";
    private final static String _CUSTOM_DIMENSION_KEY = "_CUSTOM_KEY";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_event_setup);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        mLayoutDialog = (ViewGroup) findViewById(R.id.layout_event_setup_dialog);

        // Get event name
        mEventName = getIntent().getStringExtra(KEY_EVENT_NAME);

        // Set the event name to the dialog title
        ((TextView) findViewById(R.id.text_title)).setText(String.format("Event: %s", mEventName));

        /**
         * Construct the list view with the attributes and custom dimensions
         */
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String jsonAttributes = mSharedPref.getString(mEventName + _ATTR_KEY, "");
        final String jsonCustomDimensions = mSharedPref.getString(mEventName + _CUSTOM_DIMENSION_KEY, "");

        LinkedList<Attribute> attributes = new LinkedList<Attribute>();
        if (!TextUtils.isEmpty(jsonAttributes)) {
            attributes = new Gson().fromJson(jsonAttributes, new TypeToken<LinkedList<Attribute>>(){}.getType());
        } else {
            attributes.add(new Attribute());
        }

        LinkedList<String> customDimensions = new LinkedList<String>();
        if (!TextUtils.isEmpty(jsonCustomDimensions)) {
            customDimensions = new Gson().fromJson(jsonCustomDimensions, new TypeToken<LinkedList<String>>(){}.getType());
        } else {
            customDimensions.add("");
        }

        mAttributesAdapter       = new AttributesAdapter(this, attributes);
        mCustomDimensionsAdapter = new CustomDimensionsAdapter(this, customDimensions);

        mListAttr = (ListView) findViewById(R.id.list_attributes);
        mListAttr.setAdapter(mAttributesAdapter);
        mListCustom = (ListView) findViewById(R.id.list_custom_dimensions);
        mListCustom.setAdapter(mCustomDimensionsAdapter);

        /**
         * Dynamic add edit boxes for attributes and custom dimensions input
         */
        findViewById(R.id.btn_add_attribute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAttributesAdapter.add(new Attribute());
                mAttributesAdapter.notifyDataSetChanged();
                mLayoutDialog.requestLayout();
            }
        });

        findViewById(R.id.btn_add_custom_dimension).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCustomDimensionsAdapter.add("");
                mCustomDimensionsAdapter.notifyDataSetChanged();
                mLayoutDialog.requestLayout();
            }
        });

        /**
         * Add Cancel/Save button click listeners
         */
        findViewById(R.id.btn_negative).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btn_positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkedList<Attribute> attributes = new LinkedList<Attribute>();
                for (int i = 0; i < mAttributesAdapter.getCount(); ++i) {
                    attributes.add(mAttributesAdapter.getItem(i));
                }
                LinkedList<String> customDimensions = new LinkedList<String>();
                for (int i = 0; i < mCustomDimensionsAdapter.getCount(); ++i) {
                    customDimensions.add(mCustomDimensionsAdapter.getItem(i));
                }

                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putString(mEventName + _ATTR_KEY, new Gson().toJson(attributes));
                editor.putString(mEventName + _CUSTOM_DIMENSION_KEY, new Gson().toJson(customDimensions));
                editor.commit();

                finish();
            }
        });
    }

    private static class Attribute {
        /* Attribute key */
        String mKey;

        /* Attribute value */
        String mValue;

        public Attribute() {
            this("", "");
        }

        public Attribute(String key, String value) {
            mKey   = key;
            mValue = value;
        }
    }

    public class AttributesAdapter extends ArrayAdapter<Attribute> {

        private class ViewHolder {
            EditText mEditKey;
            EditText mEditValue;
            Button   mBtnDelete;
        }

        public AttributesAdapter(final Context context, final LinkedList<Attribute> attributes) {
            super(context, R.layout.attribute_item, attributes);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // Create and cache the views if it doesn't exist
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.attribute_item, null);
                holder.mEditKey   = (EditText) convertView.findViewById(R.id.edit_key);
                holder.mEditValue = (EditText) convertView.findViewById(R.id.edit_value);
                holder.mBtnDelete = (Button) convertView.findViewById(R.id.btn_delete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Get attribute for this list item
            final Attribute attribute = getItem(position);

            // Fill the attribute key and value to the edit box
            holder.mEditKey.setText(attribute.mKey);
            holder.mEditKey.setText(attribute.mValue);
            holder.mBtnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });

            return convertView;
        }
    }

    public class CustomDimensionsAdapter extends ArrayAdapter<String> {

        private class ViewHolder {
            EditText mEditText;
            Button   mBtnDelete;
        }

        public CustomDimensionsAdapter(final Context context, final LinkedList<String> customDimensions) {
            super(context, R.layout.attribute_item, customDimensions);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // Create and cache the views if it doesn't exist
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.custom_dimension_item, null);
                holder.mEditText  = (EditText) convertView.findViewById(R.id.edit_value);
                holder.mBtnDelete = (Button) convertView.findViewById(R.id.btn_delete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.mEditText.setText(getItem(position));
            holder.mBtnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });

            return convertView;
        }
    }
}