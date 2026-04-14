package com.kintmin.log_impl.di

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.log.AppLog
import com.kintmin.log_impl.AppLogImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogImplModule {

    @Provides
    @Singleton
    fun provideLog(
        appLogRepository: AppLogRepository,
    ): AppLog {
        return AppLogImpl(appLogRepository)
    }
}
