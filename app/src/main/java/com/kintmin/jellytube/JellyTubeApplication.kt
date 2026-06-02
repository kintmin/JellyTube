package com.kintmin.jellytube

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kintmin.data.di.dataAndroidModule
import com.kintmin.data.di.dataCommonModule
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

@HiltAndroidApp
class JellyTubeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@JellyTubeApplication)
            modules(dataCommonModule, dataAndroidModule)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
