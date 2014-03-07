package com.bigbug.rocketrush.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bigbug.rocketrush.basic.AppScale;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jefflopes on 3/5/14.
 */
public class BitmapHelper {

    public static List<Bitmap> loadBitmaps(final Context context, final int[] resIDs) {
        return loadBitmaps(context, resIDs, true);
    }

    public static List<Bitmap> loadBitmaps(final Context context, final int[] resIDs, final boolean autoScale) {

        List<Bitmap> bitmaps = new ArrayList<Bitmap>();

        /*
         * Set purgeable and rgb_565 for lower memory cost and prevents OOM exception
         */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable  = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        for (int id : resIDs) {
            Bitmap origin = BitmapFactory.decodeResource(context.getResources(), id, options);
            Bitmap scaled = null;

            // scale the image according to the current screen resolution
            if (autoScale) {
                float dstWidth  = AppScale.doScaleW(origin.getWidth());
                float dstHeight = AppScale.doScaleH(origin.getHeight());

                if (dstWidth != origin.getWidth() || dstHeight != origin.getHeight()) {
                    scaled = Bitmap.createScaledBitmap(origin, (int) dstWidth, (int) dstHeight, true);
                    if (scaled != null) {
                        origin.recycle(); // Explicit release to avoid OOM
                    }
                }
            }

            // add to the image list
            bitmaps.add(scaled != null ? scaled : origin);
        }

        return bitmaps;
    }
}
