package com.ddiehl.android.reversi

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.ddiehl.android.logging.CrashlyticsLogger
import com.ddiehl.android.logging.CrashlyticsLoggingTree
import com.ddiehl.android.logging.LogcatLogger
import com.ddiehl.android.logging.LogcatLoggingTree
import io.fabric.sdk.android.Fabric
import timber.log.Timber

class Flippit : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set up logging trees
        if (BuildConfig.DEBUG) {
            Timber.plant(LogcatLoggingTree(LogcatLogger()))
        } else {
            Timber.plant(CrashlyticsLoggingTree(CrashlyticsLogger()))
        }

        // Initialize Crashlytics
        val crashlyticsCore = CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()
        val crashlytics = Crashlytics.Builder()
                .core(crashlyticsCore)
                .build()
        Fabric.with(this, crashlytics)
    }
}
