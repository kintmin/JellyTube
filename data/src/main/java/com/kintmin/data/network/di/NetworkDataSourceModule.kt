package com.kintmin.data.network.di

import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.network.dataSourceImpl.HttpDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkDataSourceModule {
    @Provides
    @Singleton
    fun provideHttpDataSource(
        defaultClient: OkHttpClient,
    ): HttpDataSource {
        return HttpDataSourceImpl(defaultClient)
    }
}