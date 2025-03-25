package com.kintmin.localdatabase.di

import com.kintmin.localdatabase.dao.AudioMediaDao
import com.kintmin.localdatabase.dao.PlaylistDao
import com.kintmin.localdatabase.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {
    @Provides
    fun provideAudioMediaDao(database: AppDatabase): AudioMediaDao {
        return database.audioMediaDao()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }
}