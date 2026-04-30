package com.kintmin.data.repository_impl.di

import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_db.dao.*
import com.kintmin.data.local_db.dao_facade.*
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.repository_impl.*
import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.step.repository.StepRepository
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
    fun provideAppLogRepository(
        fileManager: FileManager,
    ): AppLogRepository {
        return AppLogRepositoryImpl(
            fileManager = fileManager,
        )
    }

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

    @Provides
    @Singleton
    fun provideStepRepository(
        stepDao: StepDao,
        datastoreUtil: DatastoreUtil,
    ): StepRepository {
        return StepRepositoryImpl(
            stepDao = stepDao,
            datastoreUtil = datastoreUtil,
        )
    }
}
