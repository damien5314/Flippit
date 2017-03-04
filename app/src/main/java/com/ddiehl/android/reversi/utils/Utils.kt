package com.ddiehl.android.reversi.utils

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.util.Log

object Utils {
    private val TAG = Utils::class.java.simpleName

    fun displayMetrics(context: Context) {
        val metrics = context.resources.displayMetrics

        Log.d(TAG, "WIDTH: " + metrics.widthPixels.toString())
        Log.d(TAG, "HEIGHT: " + metrics.heightPixels.toString())
        Log.d(TAG, "XDPI: " + metrics.xdpi.toString())
        Log.d(TAG, "YDPI: " + metrics.ydpi.toString())

        val densityDpi: String
        if (metrics.densityDpi == DisplayMetrics.DENSITY_LOW)
            densityDpi = "LDPI (0.75)"
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM)
            densityDpi = "MDPI (1.00)"
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_HIGH)
            densityDpi = "HDPI (1.50)"
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH)
            densityDpi = "XHDPI (2.00)"
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_XXHIGH)
            densityDpi = "XXHDPI (3.00)"
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_XXXHIGH)
            densityDpi = "XXXHDPI (4.00)"
        else if (metrics.densityDpi == DisplayMetrics.DENSITY_TV)
            densityDpi = "TVDPI (1.33)"
        else
            densityDpi = metrics.densityDpi.toString()
        Log.d(TAG, "DENSITYDPI: " + densityDpi + " - " + metrics.densityDpi)

        Log.d(TAG, "DENSITY: " + metrics.density)
        Log.d(TAG, "SCALEDDENSITY: " + metrics.scaledDensity)

        var size = ""
        if (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_SMALL)
            size = "SMALL"
        else if (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_NORMAL)
            size = "NORMAL"
        else if (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE)
            size = "LARGE"
        else if (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            size = "XLARGE"
        Log.d(TAG, "SIZE: " + size)
    }

    fun byteArrayToString(array: ByteArray): String {
        val builder = StringBuilder()

        array.indices
                .map { array[it] }
                .forEach { builder.append(it.toString()) }

        return builder.toString()
    }
}
