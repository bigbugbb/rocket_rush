package com.bigbug.rocketrush.elements;

import android.content.Context;

import com.bigbug.rocketrush.basic.AppObject;

public class Utility extends AppObject {

	protected boolean mEnable;

	protected Utility(Context context) {
		super(context);
        mEnable = true;
	}

	public void setEnable(boolean enable) {
		mEnable = enable;
	}
	
	public boolean isEnabled() {
		return mEnable;
	}
}
