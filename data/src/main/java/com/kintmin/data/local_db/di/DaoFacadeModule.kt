package com.kintmin.data.local_db.di

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
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
        audioMediaDao: AudioMediaDao,
        playlistDao: PlaylistDao,
        playlistTrackDao: PlaylistTrackDao,
    ): AudioMediaFacade {
        return AudioMediaFacade(
            audioMediaDao, playlistDao, playlistTrackDao,
        )
    }
}