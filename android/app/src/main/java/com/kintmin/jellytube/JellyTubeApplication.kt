package com.kintmin.jellytube

import android.app.Application
import com.kintmin.data.di.dataAndroidModule
import com.kintmin.data.di.dataCommonModule
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import com.kintmin.jellytube.di.appModule
import com.kintmin.jellytube.di.logModule
import com.kintmin.platform.di.platformModule
import com.kintmin.platform.worker.di.workerModule
import com.kintmin.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class JellyTubeApplication : Application() {

    private val appScope = MainScope()

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

        // 액티비티는 회전·재진입마다 다시 만들어진다. 사용자 등록과 로그 설정은 프로세스당 한 번이면 된다.
        appScope.launch { get<RegisterUserUseCase>()() }
    }
}
