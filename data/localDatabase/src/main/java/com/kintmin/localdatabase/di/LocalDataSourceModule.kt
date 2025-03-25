package com.kintmin.localdatabase.di

import com.kintmin.localdatabase.dao.AudioMediaDao
import com.kintmin.localdatabase.dao.PlaylistDao
import com.kintmin.localdatabase.dataSource.LocalAudioDataSource
import com.kintmin.localdatabase.dataSource.LocalPlaylistDataSource
import com.kintmin.localdatabase.dataSourceImpl.LocalAudioDataSourceImpl
import com.kintmin.localdatabase.dataSourceImpl.LocalPlaylistDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDataSourceModule {
    @Provides
    @Singleton
    fun provideLocalAudioDataSource(
        audioMediaDao: AudioMediaDao,
    ): LocalAudioDataSource {
        return LocalAudioDataSourceImpl(audioMediaDao)
    }

    @Provides
    @Singleton
    fun provideLocalPlaylistDataSource(
        playlistDao: PlaylistDao,
    ): LocalPlaylistDataSource {
        return LocalPlaylistDataSourceImpl(playlistDao)
    }
}