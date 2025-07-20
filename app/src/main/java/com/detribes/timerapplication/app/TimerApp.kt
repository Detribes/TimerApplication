package com.detribes.timerapplication.app

import android.app.Application
import com.detribes.timerapplication.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class TimerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TimerApp)
            modules(appModule)
        }
    }
}