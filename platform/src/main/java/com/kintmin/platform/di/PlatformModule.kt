package com.kintmin.platform.di

import android.content.Context
import com.kintmin.domain.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.platform.notification.PushNotificationUtil
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.platform.util.MediaControllerManagerImpl
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
        @ApplicationContext context: Context,
    ): PushNotificationUtil {
        return PushNotificationUtil(context)
    }

    @Provides
    @Singleton
    fun bindMediaControllerManager(
        fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    ): MediaControllerManager {
        return MediaControllerManagerImpl(fetchAudioMediaListFlowUseCase)
    }
}