package com.example.offlineapidemo

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox
import timber.log.Timber

class OfflineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // LeakCanary.install(this)
        initializeLogger()
        Mapbox.getInstance(this, getString(R.string.access_token))
    }

    private fun initializeLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}