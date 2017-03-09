package com.ddiehl.android.reversi

import android.os.Handler
import android.view.animation.Animation

fun delay(ms: Long, f: () -> Unit) {
    Handler().postDelayed({
        f.invoke()
    }, ms)
}

/**
 * {@link Animation} extension function to invoke a method after the animation is finished
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
