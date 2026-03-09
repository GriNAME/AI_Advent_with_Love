package com.example.advent_11

import android.app.Application
import com.example.advent_11.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class AdventApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@AdventApplication)
            modules(appModules)
        }
    }
}
