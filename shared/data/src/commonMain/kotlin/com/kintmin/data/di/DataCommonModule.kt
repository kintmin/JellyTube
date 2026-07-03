package com.kintmin.data.di

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.DatastoreUtilImpl
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.network.createHttpClient
import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.network.dataSourceImpl.HttpDataSourceImpl
import com.kintmin.data.repository_impl.*
import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.domain.app_log.usecase.*
import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.app_setting.usecase.*
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.*
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import com.kintmin.domain.audio_play_setting.usecase.*
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.audio_track.usecase.*
import com.kintmin.domain.common_usecase.FetchLoadBalancingDelaySecondUseCase
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist.usecase.*
import com.kintmin.domain.step.repository.StepRepository
import com.kintmin.domain.step.usecase.*
import com.kintmin.domain.user.repository.UserRepository
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataCommonModule: Module = module {
    single<HttpClient> { createHttpClient() }
    single<HttpDataSource> { HttpDataSourceImpl(get()) }
    single<DatastoreUtil> { DatastoreUtilImpl(get<DataStore<Preferences>>()) }

    single { get<JellyTubeDatabase>().audioMediaDao() }
    single { get<JellyTubeDatabase>().playlistDao() }
    single { get<JellyTubeDatabase>().playlistTrackDao() }
    single { get<JellyTubeDatabase>().stepDao() }
    singleOf(::AudioMediaFacade)

    singleOf(::AppLogRepositoryImpl) bind AppLogRepository::class
    singleOf(::AppSettingRepositoryImpl) bind AppSettingRepository::class
    singleOf(::AudioMediaRepositoryImpl) bind AudioMediaRepository::class
    singleOf(::AudioPlaySettingRepositoryImpl) bind AudioPlaySettingRepository::class
    singleOf(::AudioTrackRepositoryImpl) bind AudioTrackRepository::class
    singleOf(::DeviceStatusRepositoryImpl) bind DeviceStatusRepository::class
    singleOf(::PlaylistRepositoryImpl) bind PlaylistRepository::class
    singleOf(::StepRepositoryImpl) bind StepRepository::class
    singleOf(::UserRepositoryImpl) bind UserRepository::class

    factoryOf(::FetchAppLogDateListUseCase)
    factoryOf(::FetchAppLogLineListUseCase)
    factoryOf(::FetchIsStepEnabledFlowUseCase)
    factoryOf(::FetchPlaylistIdOnDownloadFlowUseCase)
    factoryOf(::FetchShouldInsertAtTopOnDownloadFlowUseCase)
    factoryOf(::UpdateIsStepEnabledUseCase)
    factoryOf(::UpdatePlaylistIdOnDownloadUseCase)
    factoryOf(::UpdateShouldInsertAtTopOnDownloadUseCase)
    factoryOf(::CleanupAnomalousDataUseCase)
    factoryOf(::DeleteAudioMediaListUseCase)
    factoryOf(::DeleteAudioMediaUseCase)
    factoryOf(::DownloadAudioMediaUseCase)
    factoryOf(::ImportSharedAudioMediaUseCase)
    factoryOf(::ImportUploadedAudioMediaUseCase)
    factoryOf(::SaveAudioMediaImageUseCase)
    factoryOf(::UpdateAudioMediaUseCase)
    factoryOf(::FetchIsPlaybackRepeatingFlowUseCase)
    factoryOf(::FetchIsPlaybackShufflingFlowUseCase)
    factoryOf(::FetchPlaybackPitchSemitoneFlowUseCase)
    factoryOf(::FetchPlaybackSpeedFlowUseCase)
    factoryOf(::UpdateIsPlaybackShufflingUseCase)
    factoryOf(::UpdatePlaybackPitchSemitoneUseCase)
    factoryOf(::UpdatePlaybackRepeatingUseCase)
    factoryOf(::UpdatePlaybackSpeedUseCase)
    factoryOf(::AddAudioMediaListToPlaylistUseCase)
    factoryOf(::DeleteAudioTrackListUseCase)
    factoryOf(::ToggleFavoriteUseCase)
    factoryOf(::FetchAudioMediaDetailFlowUseCase)
    factoryOf(::FetchAudioMediaListFlowUseCase)
    factoryOf(::FetchAudioMediaListToAddTrackFlowUseCase)
    factoryOf(::UpdateTrackSequenceUseCase)
    factoryOf(::FetchLoadBalancingDelaySecondUseCase)
    factoryOf(::EnsureSystemPlaylistsUseCase)
    factoryOf(::FetchPlaylistsByTypeUseCase)
    factoryOf(::AddNewPlaylistUseCase)
    factoryOf(::DeletePlaylistUseCase)
    factoryOf(::FetchAllPlaylistFlowUseCase)
    factoryOf(::FetchPlaylistFlowUseCase)
    factoryOf(::UpdatePlaylistDescriptionUseCase)
    factoryOf(::UpdatePlaylistTitleUseCase)
    factoryOf(::UpdatePlaylistSequenceUseCase)
    factoryOf(::BackupStepSensorUseCase)
    factoryOf(::CalculateStepCountUseCase)
    factoryOf(::GetAccelerateStepUseCase)
    factoryOf(::GetHalfHourlyStepsUseCase)
    factoryOf(::GetLastStepSensorForTodayUseCase)
    factoryOf(::GetMonthlyDailyStepsUseCase)
    factoryOf(::GetStepCountUseCase)
    factoryOf(::RegisterDailyResetWorkerUseCase)
    factoryOf(::ResetDataOncePerDayUseCase)
    factoryOf(::UpdateAccelerateStepUseCase)
    factoryOf(::UpdateLastStepSensorUseCase)
    factoryOf(::RegisterUserUseCase)
}
