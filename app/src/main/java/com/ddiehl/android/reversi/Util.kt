package com.ddiehl.android.reversi

import android.os.Handler

fun delay(ms: Long, f: () -> Unit) {
    Handler().postDelayed({
        f.invoke()
    }, ms)
}