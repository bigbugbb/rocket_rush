package com.bigbug.rocketrush.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.bigbug.rocketrush.R;

public class TextViewShadowDips extends TextView {

    public TextViewShadowDips(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.TextViewShadowDips);
        final float shadowRadius = attributes.getDimension(R.styleable.TextViewShadowDips_shadowRadius, 0f);
        final float shadowDx = attributes.getDimension(R.styleable.TextViewShadowDips_shadowDx, 0f);
        final float shadowDy = attributes.getDimension(R.styleable.TextViewShadowDips_shadowDy, 0f);
        final int shadowColor = attributes.getColor(R.styleable.TextViewShadowDips_shadowColor, 0);
        if(shadowColor != 0) {
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
        } else {
            getPaint().clearShadowLayer();
        }
        attributes.recycle();
    }

}
