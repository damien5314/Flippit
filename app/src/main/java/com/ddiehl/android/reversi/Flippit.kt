package com.ddiehl.android.reversi

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import timber.log.Timber

class Flippit : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set up logging trees
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Crashlytics
        Fabric.with(this, Crashlytics())
    }
}
