package com.kintmin.data.di

import com.kintmin.data.local_db.dataSource.LocalAudioDataSource
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.repository_impl.AudioMediaRepositoryImpl
import com.kintmin.domain.repository.AudioMediaRepository
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