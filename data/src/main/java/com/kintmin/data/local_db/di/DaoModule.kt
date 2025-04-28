package com.kintmin.data.local_db.di

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {
    @Provides
    fun provideAudioMediaDao(database: JellyTubeDatabase): AudioMediaDao {
        return database.audioMediaDao()
    }

    @Provides
    fun providePlaylistDao(database: JellyTubeDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun providePlaylistTrackDao(database: JellyTubeDatabase): PlaylistTrackDao {
        return database.playlistTrackDao()
    }
}