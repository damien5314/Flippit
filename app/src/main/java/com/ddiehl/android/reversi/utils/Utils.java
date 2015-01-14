package com.ddiehl.android.reversi.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Created by ddiehl001c on 12/21/2014.
 */
public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static void displayMetrics(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        Log.d(TAG, "WIDTH: " + String.valueOf(metrics.widthPixels));
        Log.d(TAG, "HEIGHT: " + String.valueOf(metrics.heightPixels));
        Log.d(TAG, "XDPI: " + String.valueOf(metrics.xdpi));
        Log.d(TAG, "YDPI: " + String.valueOf(metrics.ydpi));

        String densityDpi;
        if (metrics.densityDpi == DisplayMetrics.DENSITY_LOW) densityDpi = "LDPI (0.75)";
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM) densityDpi = "MDPI (1.00)";
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_HIGH) densityDpi = "HDPI (1.50)";
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) densityDpi = "XHDPI (2.00)";
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_XXHIGH) densityDpi = "XXHDPI (3.00)";
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_XXXHIGH) densityDpi = "XXXHDPI (4.00)";
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_TV) densityDpi = "TVDPI (1.33)";
        else densityDpi = String.valueOf(metrics.densityDpi);
        Log.d(TAG, "DENSITYDPI: " + densityDpi + " - " + metrics.densityDpi);

        Log.d(TAG, "DENSITY: " + metrics.density);
        Log.d(TAG, "SCALEDDENSITY: " + metrics.scaledDensity);

        String size = "";
        if ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_SMALL) size = "SMALL";
        else if ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_NORMAL) size = "NORMAL";
        else if ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_LARGE) size = "LARGE";
        else if ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE) size = "XLARGE";
        Log.d(TAG, "SIZE: " + size);
    }

}
