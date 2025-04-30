package com.kintmin.platform.di

import android.content.Context
import com.kintmin.platform.notification.PushNotificationUtil
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
    fun providePushNotificationUtil(
        @ApplicationContext context: Context
    ): PushNotificationUtil {
        return PushNotificationUtil(context)
    }
}