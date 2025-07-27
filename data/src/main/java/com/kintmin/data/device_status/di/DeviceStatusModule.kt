package com.kintmin.data.device_status.di

import android.content.Context
import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.device_status.DeviceStatusImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DeviceStatusModule {

    @Provides
    @Singleton
    fun provideDatastore(
        @ApplicationContext context: Context,
    ): DeviceStatus = DeviceStatusImpl(context)
}