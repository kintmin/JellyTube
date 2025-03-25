package com.kintmin.dataapi.di

import com.kintmin.dataapi.repositoryImpl.AudioMediaRepositoryImpl
import com.kintmin.domain.repository.AudioMediaRepository
import com.kintmin.localdatabase.dataSource.LocalAudioDataSource
import com.kintmin.localfile.FileManager
import com.kintmin.network.dataSource.HttpDataSource
import com.kintmin.pythonbridge.PythonExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideDatabase(
        localAudioDataSource: LocalAudioDataSource,
        httpDataSource: HttpDataSource,
        fileManager: FileManager,
        pythonExecutor: PythonExecutor,
    ): AudioMediaRepository {
        return AudioMediaRepositoryImpl(
            localAudioDataSource,
            httpDataSource,
            fileManager,
            pythonExecutor,
        )
    }
}