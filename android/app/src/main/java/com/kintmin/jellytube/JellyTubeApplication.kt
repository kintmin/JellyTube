package com.kintmin.jellytube

import android.app.Application
import com.kintmin.data.di.dataAndroidModule
import com.kintmin.data.di.dataCommonModule
import com.kintmin.jellytube.di.appModule
import com.kintmin.jellytube.di.logModule
import com.kintmin.platform.di.platformModule
import com.kintmin.platform.worker.di.workerModule
import com.kintmin.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class JellyTubeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@JellyTubeApplication)
            workManagerFactory()
            modules(
                dataCommonModule,
                dataAndroidModule,
                logModule,
                platformModule,
                workerModule,
                presentationModule,
                appModule,
            )
        }
    }
}
