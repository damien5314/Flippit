package com.ddiehl.android.reversi

import android.app.Application
import com.ddiehl.android.logging.LogcatLogger
import com.ddiehl.android.logging.LogcatLoggingTree
import timber.log.Timber

class Flippit : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set up logging trees
        if (BuildConfig.DEBUG) {
            Timber.plant(LogcatLoggingTree(LogcatLogger()))
        }
    }
}