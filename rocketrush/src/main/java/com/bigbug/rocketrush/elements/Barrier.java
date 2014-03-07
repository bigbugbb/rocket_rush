package com.bigbug.rocketrush.elements;

import android.content.Context;

import com.bigbug.rocketrush.basic.AppCtrl;
import com.bigbug.rocketrush.basic.AppObject;

public class Barrier extends AppObject {

	protected int mCanvasWidth  = 0;
	protected int mCanvasHeight = 0;
	protected int mAccMoveDuration = 0;
	
	protected Barrier(Context context) {
		super(context);
	}

	public void triggerCollideEffect(int kind, float x, float y) {}
	
	@Override
	public void operate(AppCtrl ctrl) {
		int command = ctrl.getCommand();
		
		if (command == AppCtrl.MOVE_VERT) {
			mAccMoveDuration = 1000;
		} 
	}

	@Override
	public void onSizeChanged(int width, int height) {
		mCanvasWidth  = width;
		mCanvasHeight = height;		
	}
}
