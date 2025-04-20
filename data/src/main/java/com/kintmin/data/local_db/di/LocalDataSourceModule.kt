package com.kintmin.data.local_db.di

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dataSource.LocalAudioDataSource
import com.kintmin.data.local_db.dataSource.LocalPlaylistDataSource
import com.kintmin.data.local_db.dataSourceImpl.LocalAudioDataSourceImpl
import com.kintmin.data.local_db.dataSourceImpl.LocalPlaylistDataSourceImpl
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