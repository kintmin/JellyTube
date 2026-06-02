package com.kintmin.platform.di

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.PushNotificationManagerImpl
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.MediaControllerManagerImpl
import com.kintmin.platform.worker.usecase.ExecuteYoutubeDownload
import com.kintmin.platform.worker.usecase.RegisterDailyResetImmediatelyWorkerImpl
import com.kintmin.platform.worker.usecase.RegisterLoadBalancedDailyResetWorkerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val platformModule: Module = module {
    single<PushNotificationManager> { PushNotificationManagerImpl(androidContext()) }
    single<MediaControllerManager> { MediaControllerManagerImpl(androidContext()) }
    single<RegisterDailyResetImmediatelyWorker> {
        RegisterDailyResetImmediatelyWorkerImpl(androidContext())
    }
    single<RegisterLoadBalancedDailyResetWorker> {
        RegisterLoadBalancedDailyResetWorkerImpl(androidContext(), get())
    }
    single { ExecuteYoutubeDownload(androidContext()) }
}
