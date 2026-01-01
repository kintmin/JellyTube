package com.kintmin.data.local_db.di

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DaoFacadeModule {

    @Provides
    @Singleton
    fun provideAudioMediaFacade(
        db: JellyTubeDatabase,
        audioMediaDao: AudioMediaDao,
        playlistDao: PlaylistDao,
        playlistTrackDao: PlaylistTrackDao,
    ): AudioMediaFacade {
        return AudioMediaFacade(
            db, audioMediaDao, playlistDao, playlistTrackDao,
        )
    }
}