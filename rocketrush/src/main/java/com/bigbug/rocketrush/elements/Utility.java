package com.bigbug.rocketrush.elements;

import android.content.res.Resources;

import com.bigbug.rocketrush.basic.AppObject;

public class Utility extends AppObject {
	protected boolean mEnable = true;

	protected Utility(Resources res) {
		super(res);
	}

	public void setEnable(boolean enable) {
		mEnable = enable;
	}
	
	public boolean isEnabled() {
		return mEnable;
	}
}
