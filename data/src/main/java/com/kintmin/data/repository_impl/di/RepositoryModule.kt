package com.kintmin.data.repository_impl.di

import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.repository_impl.AudioMediaRepositoryImpl
import com.kintmin.data.repository_impl.AudioPlaySettingRepositoryImpl
import com.kintmin.data.repository_impl.AudioTrackRepositoryImpl
import com.kintmin.data.repository_impl.DeviceStatusRepositoryImpl
import com.kintmin.data.repository_impl.PlaylistRepositoryImpl
import com.kintmin.data.repository_impl.AppSettingRepositoryImpl
import com.kintmin.data.repository_impl.UserRepositoryImpl
import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.device.repository.DeviceStatusRepository
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
    fun provideAppSettingRepository(
        datastoreUtil: DatastoreUtil,
    ): AppSettingRepository {
        return AppSettingRepositoryImpl(
            datastoreUtil = datastoreUtil,
        )
    }

    @Provides
    @Singleton
    fun provideAudioMediaRepository(
        audioMediaFacade: AudioMediaFacade,
        audioMediaDao: AudioMediaDao,
        httpDataSource: HttpDataSource,
        fileManager: FileManager,
        pythonExecutor: PythonExecutor,
    ): AudioMediaRepository {
        return AudioMediaRepositoryImpl(
            audioMediaFacade = audioMediaFacade,
            audioMediaDao = audioMediaDao,
            httpDataSource = httpDataSource,
            fileManager = fileManager,
            pythonExecutor = pythonExecutor,
        )
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        audioMediaFacade: AudioMediaFacade,
        playlistDao: PlaylistDao,
        fileManager: FileManager,
    ): PlaylistRepository {
        return PlaylistRepositoryImpl(
            audioMediaFacade = audioMediaFacade,
            playlistDao = playlistDao,
            fileManager = fileManager,
        )
    }

    @Provides
    @Singleton
    fun provideAudioTrackRepository(
        audioMediaFacade: AudioMediaFacade,
        fileManager: FileManager,
        playlistTrackDao: PlaylistTrackDao,
    ): AudioTrackRepository {
        return AudioTrackRepositoryImpl(
            audioMediaFacade = audioMediaFacade,
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

    @Provides
    @Singleton
    fun provideDeviceStatusRepository(
        deviceStatus: DeviceStatus,
    ): DeviceStatusRepository {
        return DeviceStatusRepositoryImpl(
            deviceStatus = deviceStatus,
        )
    }
}
