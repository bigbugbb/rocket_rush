package com.bigbug.rocketrush.sdktest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.bigbug.rocketrush.Globals;
import com.bigbug.rocketrush.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EventSetupDialog extends FragmentActivity {

    public static final String KEY_EVENT_NAME = "KEY_EVENT_NAME";

    private ViewGroup mLayoutDialog;

    private ListView mListAttr;

    private ListView mListCustom;

    private AttributesAdapter mAttributesAdapter;

    private CustomDimensionsAdapter mCustomDimensionsAdapter;

    private SharedPreferences mSharedPref;

    private String mEventName;

    private LinkedList<Pair<String, String>> mAttributesData;

    private LinkedList<String> mCustomDimensionsData;

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
        final String jsonAttributes = mSharedPref.getString(mEventName + Globals._ATTR_KEY, "");
        final String jsonCustomDimensions = mSharedPref.getString(mEventName + Globals._CUSTOM_DIMENSION_KEY, "");

        if (!TextUtils.isEmpty(jsonAttributes)) {
            mAttributesData = new Gson().fromJson(jsonAttributes, new TypeToken<LinkedList<Pair<String, String>>>(){}.getType());
        } else {
            mAttributesData = new LinkedList<Pair<String, String>>();
            mAttributesData.add(new Pair<String, String>("", ""));
        }

        if (!TextUtils.isEmpty(jsonCustomDimensions)) {
            mCustomDimensionsData = new Gson().fromJson(jsonCustomDimensions, new TypeToken<LinkedList<String>>(){}.getType());
        } else {
            mCustomDimensionsData = new LinkedList<String>();
            mCustomDimensionsData.add("");
        }

        mAttributesAdapter       = new AttributesAdapter(this, mAttributesData);
        mCustomDimensionsAdapter = new CustomDimensionsAdapter(this, mCustomDimensionsData);

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
                mAttributesData.add(new Pair<String, String>("", ""));
                mAttributesAdapter.notifyDataSetChanged();
            }
        });

        findViewById(R.id.btn_add_custom_dimension).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCustomDimensionsData.add("");
                mCustomDimensionsAdapter.notifyDataSetChanged();
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
                LinkedList<Pair<String, String>> attributes = new LinkedList<Pair<String, String>>();
                for (int i = 0; i < mAttributesAdapter.getCount(); ++i) {
                    attributes.add(mAttributesAdapter.getItem(i));
                }
                LinkedList<String> customDimensions = new LinkedList<String>();
                for (int i = 0; i < mCustomDimensionsAdapter.getCount(); ++i) {
                    customDimensions.add(mCustomDimensionsAdapter.getItem(i));
                }

                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putString(mEventName + Globals._ATTR_KEY, new Gson().toJson(attributes));
                editor.putString(mEventName + Globals._CUSTOM_DIMENSION_KEY, new Gson().toJson(customDimensions));
                editor.commit();

                finish();
            }
        });
    }

    private void updateAttributeData() {
        for (int i = 0; i < mListAttr.getCount(); ++i) {
            View view = mListAttr.getChildAt(i);
            if (view != null) {
                int firstVisiblePosition = mListAttr.getFirstVisiblePosition();
                EditText editKey   = (EditText) view.findViewById(R.id.edit_key);
                EditText editValue = (EditText) view.findViewById(R.id.edit_value);
                mAttributesData.set(i + firstVisiblePosition, new Pair<String, String>(editKey.getText().toString(), editValue.getText().toString()));
            }
        }
    }

    private void updateCustomDimensionData() {
        for (int i = 0; i < mListCustom.getCount(); ++i) {
            View view = mListCustom.getChildAt(i);
            if (view != null) {
                int firstVisiblePosition = mListCustom.getFirstVisiblePosition();
                EditText edit = (EditText) view.findViewById(R.id.edit_value);
                String s = edit.getText().toString();
                mCustomDimensionsData.set(i + firstVisiblePosition, s);
            }
        }
    }

    private void deleteAttributeByView(View v) {
        int target = 0;
        for (int i = 0; i < mListCustom.getCount(); ++i) {
            View item = mListCustom.getChildAt(i);
            ViewHolder holder = (ViewHolder) item.getTag();
            if (holder != null) {
                if (holder.mViews.get(1) == v) {
                    target = i;
                    break;
                }
            }
        }
        mAttributesData.remove(target);
        mAttributesAdapter.notifyDataSetChanged();
    }

    private void deleteCustomDimensionByView(View v) {
        int target = 0;
        for (int i = 0; i < mListCustom.getCount(); ++i) {
            View item = mListCustom.getChildAt(i);
            ViewHolder holder = (ViewHolder) item.getTag();
            if (holder != null) {
                if (holder.mViews.get(1) == v) {
                    target = i;
                    break;
                }
            }
        }
        mCustomDimensionsData.remove(target);
        mCustomDimensionsAdapter.notifyDataSetChanged();
    }

    class ViewHolder {
        List<View> mViews = new ArrayList<View>();
    }

    public class AttributesAdapter extends ArrayAdapter<Pair<String, String>> {

        public AttributesAdapter(final Context context, final LinkedList<Pair<String, String>> attributes) {
            super(context, R.layout.attribute_item, attributes);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // Create and cache the views if it doesn't exist
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.attribute_item, null);
                holder.mViews.add(0, convertView.findViewById(R.id.edit_key));
                holder.mViews.add(1, convertView.findViewById(R.id.edit_value));
                holder.mViews.add(2, convertView.findViewById(R.id.btn_delete));
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Get attribute for this list item
            final Pair<String, String> attribute = getItem(position);

            // Fill the attribute key and value to the edit box
            ((EditText) holder.mViews.get(0)).setText(attribute.first);
            ((EditText) holder.mViews.get(0)).addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    updateAttributeData();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
            });

            ((EditText) holder.mViews.get(1)).setText(attribute.second);
            ((EditText) holder.mViews.get(1)).addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    updateAttributeData();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
            });

            holder.mViews.get(2).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAttributeByView(v);
                }
            });

            return convertView;
        }
    }

    public class CustomDimensionsAdapter extends ArrayAdapter<String> {

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
                holder.mViews.add(0, convertView.findViewById(R.id.edit_value));
                holder.mViews.add(1, convertView.findViewById(R.id.btn_delete));
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String value = getItem(position);
            ((EditText) holder.mViews.get(0)).setText(value);
            ((EditText) holder.mViews.get(0)).addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    updateCustomDimensionData();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
            });

            holder.mViews.get(1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteCustomDimensionByView(v);
                }
            });

            return convertView;
        }
    }
}