package com.kintmin.platform.di

import android.content.Context
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.PushNotificationManagerImpl
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.MediaControllerManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun bindPushNotificationManager(
        @ApplicationContext context: Context,
    ): PushNotificationManager {
        return PushNotificationManagerImpl(context)
    }

    @Provides
    @Singleton
    fun bindMediaControllerManager(
        @ApplicationContext context: Context,
    ): MediaControllerManager {
        return MediaControllerManagerImpl(context)
    }
}