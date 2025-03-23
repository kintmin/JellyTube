package com.kintmin.data.local

import android.content.Context
import androidx.room.Room
import com.kintmin.data.local.dao.AudioMediaDao
import com.kintmin.data.local.datasource.FileManager
import com.kintmin.data.local.datasource.LocalAudioDataSource
import com.kintmin.data.local.datasource.PythonExecutor
import com.kintmin.ytmusicbox.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DB_NAME,
    ).build()

    @Provides
    fun provideYoutubeMediaDao(database: AppDatabase): AudioMediaDao {
        return database.audioMediaDao()
    }

    @Provides
    @Singleton
    fun provideFileManager(@ApplicationContext context: Context): FileManager {
        return FileManager(context)
    }

    @Provides
    @Singleton
    fun provideLocalAudioDataSource(
        audioMediaDao: AudioMediaDao,
    ): LocalAudioDataSource {
        return LocalAudioDataSource(audioMediaDao)
    }

    @Provides
    @Singleton
    fun providePythonExecutor(
        @ApplicationContext context: Context,
    ): PythonExecutor {
        return PythonExecutor(context)
    }
}