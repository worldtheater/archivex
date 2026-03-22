package com.worldtheater.archive

import android.app.Application
import com.worldtheater.archive.di.appModule
import com.worldtheater.archive.platform.auth.DefaultSensitiveAuthSessionStore
import com.worldtheater.archive.util.log.AndroidLogger
import com.worldtheater.archive.util.log.L
import com.worldtheater.archive.util.log.L.LEVEL_ERROR
import com.worldtheater.archive.util.log.L.LEVEL_MASK_ALL
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeAndroidApp(this)
    }

    private fun initializeAndroidApp(application: Application) {
        val releaseMask = LEVEL_ERROR
        L.init(if (BuildConfig.DEBUG) LEVEL_MASK_ALL else releaseMask, AndroidLogger("ArchiveApp"))
        System.setProperty("kotlinx.coroutines.scheduler.max.pool.size", 64.toString())
        AppContextHolder.init(application)
        startKoin {
            androidContext(application)
            modules(appModule)
        }
        DefaultSensitiveAuthSessionStore.reset()
    }

}
