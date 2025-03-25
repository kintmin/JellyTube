package com.kintmin.notification.di

import android.content.Context
import com.kintmin.notification.PushNotificationUtil
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
    ): PushNotificationUtil {
        return PushNotificationUtil(context)
    }
}