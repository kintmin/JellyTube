package com.kintmin.presentation

import android.content.Context
import com.kintmin.presentation.notification.NotificationUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PresentationModule {
    @Provides
    @Singleton
    fun provideNotificationUtil(
        @ApplicationContext context: Context
    ): NotificationUtil {
        return NotificationUtil(context)
    }
}