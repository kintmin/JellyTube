package com.kintmin.data.repository_impl.di

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.repository_impl.AudioMediaRepositoryImpl
import com.kintmin.data.repository_impl.AudioPlaySettingRepositoryImpl
import com.kintmin.data.repository_impl.AudioTrackRepositoryImpl
import com.kintmin.data.repository_impl.PlaylistRepositoryImpl
import com.kintmin.data.repository_impl.UserRepositoryImpl
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.user.repository.UserRepository
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
        playlistTrackDao: PlaylistTrackDao,
        fileManager: FileManager,
    ): PlaylistRepository {
        return PlaylistRepositoryImpl(
            playlistDao = playlistDao,
            playlistTrackDao = playlistTrackDao,
            fileManager = fileManager,
        )
    }

    @Provides
    @Singleton
    fun provideAudioTrackRepository(
        fileManager: FileManager,
        playlistTrackDao: PlaylistTrackDao,
    ): AudioTrackRepository {
        return AudioTrackRepositoryImpl(
            fileManager = fileManager,
            playlistTrackDao = playlistTrackDao,
        )
    }

    @Provides
    @Singleton
    fun provideAudioPlaySettingRepository(
        datastoreUtil: DatastoreUtil,
    ): AudioPlaySettingRepository {
        return AudioPlaySettingRepositoryImpl(
            datastoreUtil = datastoreUtil,
        )
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        datastoreUtil: DatastoreUtil,
    ): UserRepository {
        return UserRepositoryImpl(
            datastoreUtil = datastoreUtil,
        )
    }
}