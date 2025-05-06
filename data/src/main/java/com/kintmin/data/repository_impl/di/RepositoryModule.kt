package com.kintmin.data.repository_impl.di

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.repository_impl.AudioMediaRepositoryImpl
import com.kintmin.data.repository_impl.PlaybackRepositoryImpl
import com.kintmin.data.repository_impl.PlaylistRepositoryImpl
import com.kintmin.domain.repository.AudioMediaRepository
import com.kintmin.domain.repository.PlaybackRepository
import com.kintmin.domain.repository.PlaylistRepository
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
    fun provideAudioMediaRepository(
        audioMediaDao: AudioMediaDao,
        playlistTrackDao: PlaylistTrackDao,
        httpDataSource: HttpDataSource,
        fileManager: FileManager,
        pythonExecutor: PythonExecutor,
    ): AudioMediaRepository {
        return AudioMediaRepositoryImpl(
            audioMediaDao = audioMediaDao,
            playlistTrackDao = playlistTrackDao,
            httpDataSource = httpDataSource,
            fileManager = fileManager,
            pythonExecutor = pythonExecutor,
        )
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        playlistDao: PlaylistDao,
        fileManager: FileManager,
    ): PlaylistRepository {
        return PlaylistRepositoryImpl(
            playlistDao = playlistDao,
            fileManager = fileManager,
        )
    }

    @Provides
    @Singleton
    fun providePlaybackRepository(
        datastoreUtil: DatastoreUtil,
        playlistTrackDao: PlaylistTrackDao,
    ): PlaybackRepository {
        return PlaybackRepositoryImpl(
            datastoreUtil = datastoreUtil,
            playlistTrackDao = playlistTrackDao,
        )
    }
}