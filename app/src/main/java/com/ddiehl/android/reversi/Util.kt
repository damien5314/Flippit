package com.ddiehl.android.reversi

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.support.annotation.ColorInt
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.widget.ImageView
import timber.log.Timber

fun delay(ms: Long, f: () -> Unit) {
    Handler().postDelayed({
        f.invoke()
    }, ms)
}

/**
 * [Animation] extension function to invoke a method after the animation is finished
 */
fun Animation.onAnimationEnd(f: () -> Unit) {
    setAnimationListener(
            object: Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) { }
                override fun onAnimationStart(animation: Animation?) { }

                override fun onAnimationEnd(animation: Animation?) {
                    f.invoke()
                }
            }
    )
}

fun displayMetrics(context: Context) {
    val metrics = context.resources.displayMetrics

    Timber.d("WIDTH: %d", metrics.widthPixels.toString())
    Timber.d("HEIGHT: %d", metrics.heightPixels.toString())
    Timber.d("XDPI: %f", metrics.xdpi.toString())
    Timber.d("YDPI: %f", metrics.ydpi.toString())

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
    Timber.d("DENSITYDPI: %s - %d", densityDpi, metrics.densityDpi)

    Timber.d("DENSITY: %f", metrics.density)
    Timber.d("SCALEDDENSITY: %f", metrics.scaledDensity)

    val config = context.resources.configuration
    val size =
            if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
                    == Configuration.SCREENLAYOUT_SIZE_SMALL)
                "SMALL"
            else if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
                    == Configuration.SCREENLAYOUT_SIZE_NORMAL)
                "NORMAL"
            else if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
                    == Configuration.SCREENLAYOUT_SIZE_LARGE)
                "LARGE"
            else if (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
                    == Configuration.SCREENLAYOUT_SIZE_XLARGE)
                "XLARGE"
            else "UNKNOWN"
    Timber.d("SIZE: %s", size)
}

fun byteArrayToString(array: ByteArray): String {
    val builder = StringBuilder()

    array.indices
            .map { array[it] }
            .forEach { builder.append(it.toString()) }

    return builder.toString()
}


/**
 * Tints menu icons similar to the AppCompat style
 *
 *
 * http://stackoverflow.com/a/33697621/3238938
 */
object MenuTintUtils {

    fun tintAllIcons(menu: Menu, @ColorInt color: Int) {
        for (i in 0..menu.size() - 1) {
            val item = menu.getItem(i)
            tintMenuItemIcon(color, item)
            tintShareIconIfPresent(color, item)
        }
    }

    private fun tintMenuItemIcon(@ColorInt color: Int, item: MenuItem) {
        val drawable = item.icon
        if (drawable != null) {
            val wrapped = DrawableCompat.wrap(drawable)
            drawable.mutate()
            DrawableCompat.setTint(wrapped, color)
            item.icon = drawable
        }
    }

    private fun tintShareIconIfPresent(@ColorInt color: Int, item: MenuItem) {
        if (item.actionView != null) {
            val actionView = item.actionView
            val expandActivitiesButton = actionView.findViewById(R.id.expand_activities_button)
            if (expandActivitiesButton != null) {
                val image = expandActivitiesButton.findViewById(R.id.image) as ImageView
                val drawable = image.drawable
                val wrapped = DrawableCompat.wrap(drawable)
                drawable.mutate()
                DrawableCompat.setTint(wrapped, color)
                image.setImageDrawable(drawable)
            }
        }
    }
}
