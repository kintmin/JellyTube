package com.kintmin.platform.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides
    @Singleton
    fun providePushNotificationUtil(
        @ApplicationContext context: Context
    ): com.kintmin.platform.notification.PushNotificationUtil {
        return com.kintmin.platform.notification.PushNotificationUtil(context)
    }
}