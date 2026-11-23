package com.kintmin.data.di

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.DatastoreUtilImpl
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.network.createHttpClient
import com.kintmin.data.network.data_source.HttpDataSource
import com.kintmin.data.network.data_source.KaraokeDataSource
import com.kintmin.data.network.data_source.GitHubReleaseDataSource
import com.kintmin.data.network.data_source.LyricsDataSource
import com.kintmin.data.network.data_source_impl.HttpDataSourceImpl
import com.kintmin.data.network.data_source_impl.KaraokeDataSourceImpl
import com.kintmin.data.network.data_source_impl.GitHubReleaseDataSourceImpl
import com.kintmin.data.network.data_source_impl.LyricsDataSourceImpl
import com.kintmin.data.repository_impl.*
import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.domain.app_log.usecase.*
import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.app_setting.usecase.*
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_edit.usecase.*
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.*
import com.kintmin.domain.file_share.usecase.CreatePcUploadHandlerUseCase
import com.kintmin.domain.file_share.usecase.FetchPcFileShareServerRunningUseCase
import com.kintmin.domain.file_share.usecase.StartPcFileShareServerUseCase
import com.kintmin.domain.file_share.usecase.StopPcFileShareServerUseCase
import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import com.kintmin.domain.audio_play_setting.usecase.*
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.audio_track.usecase.*
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.backup.usecase.*
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.data_migration.usecase.*
import com.kintmin.domain.karaoke.repository.KaraokeRepository
import com.kintmin.domain.app_update.repository.AppUpdateRepository
import com.kintmin.domain.app_update.usecase.*
import com.kintmin.domain.karaoke.usecase.*
import com.kintmin.domain.lyrics.repository.LyricsRepository
import com.kintmin.domain.lyrics.repository.LyricsTranslationRepository
import com.kintmin.domain.lyrics.usecase.*
import com.kintmin.domain.common_usecase.BuildSongSearchQueryUseCase
import com.kintmin.domain.common_usecase.FetchLoadBalancingDelaySecondUseCase
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.device.usecase.FetchConnectionStatusUseCase
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeMinutesFlowUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.StartSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.UpdateSecretModeMinutesUseCase
import com.kintmin.domain.playlist.usecase.*
import com.kintmin.domain.playlist_download.usecase.DownloadYoutubePlaylistUseCase
import com.kintmin.domain.playlist_download.usecase.PreviewPlaylistDownloadUseCase
import com.kintmin.domain.step.repository.StepRepository
import com.kintmin.domain.step.usecase.*
import com.kintmin.domain.user.repository.UserRepository
import com.kintmin.domain.user.usecase.FetchUserIdUseCase
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import io.ktor.client.HttpClient
import com.kintmin.data.log.SentryAppLog
import com.kintmin.log.AppLog
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataCommonModule: Module = module {
    single<HttpClient> { createHttpClient() }
    single<HttpDataSource> { HttpDataSourceImpl(get()) }
    single<LyricsDataSource> { LyricsDataSourceImpl(get()) }
    single<KaraokeDataSource> { KaraokeDataSourceImpl(get()) }
    single<GitHubReleaseDataSource> { GitHubReleaseDataSourceImpl(get()) }
    single<DatastoreUtil> { DatastoreUtilImpl(get<DataStore<Preferences>>()) }

    single { get<JellyTubeDatabase>().audioMediaDao() }
    single { get<JellyTubeDatabase>().playlistDao() }
    single { get<JellyTubeDatabase>().playlistTrackDao() }
    single { get<JellyTubeDatabase>().stepDao() }
    single { get<JellyTubeDatabase>().audioEditProjectDao() }
    singleOf(::AudioMediaFacade)

    singleOf(::AppLogRepositoryImpl) bind AppLogRepository::class
    singleOf(::SentryAppLog) bind AppLog::class
    singleOf(::AppSettingRepositoryImpl) bind AppSettingRepository::class
    singleOf(::AudioEditRepositoryImpl) bind AudioEditRepository::class
    singleOf(::AudioMediaRepositoryImpl) bind AudioMediaRepository::class
    singleOf(::AudioPlaySettingRepositoryImpl) bind AudioPlaySettingRepository::class
    singleOf(::AudioTrackRepositoryImpl) bind AudioTrackRepository::class
    singleOf(::BackupRepositoryImpl) bind BackupRepository::class
    singleOf(::DataMigrationRepositoryImpl) bind DataMigrationRepository::class
    singleOf(::DeviceStatusRepositoryImpl) bind DeviceStatusRepository::class
    singleOf(::LyricsRepositoryImpl) bind LyricsRepository::class
    singleOf(::LyricsTranslationRepositoryImpl) bind LyricsTranslationRepository::class
    singleOf(::KaraokeRepositoryImpl) bind KaraokeRepository::class
    singleOf(::AppUpdateRepositoryImpl) bind AppUpdateRepository::class
    singleOf(::PlaylistRepositoryImpl) bind PlaylistRepository::class
    singleOf(::SecretModeRepositoryImpl) bind SecretModeRepository::class
    singleOf(::StepRepositoryImpl) bind StepRepository::class
    singleOf(::UserRepositoryImpl) bind UserRepository::class

    factoryOf(::FetchAppLogDateListUseCase)
    factoryOf(::FetchAppLogLineListUseCase)
    factoryOf(::FetchAppLogFileFullPathsUseCase)
    factoryOf(::FetchIsStepEnabledFlowUseCase)
    factoryOf(::FetchPlaylistIdOnDownloadFlowUseCase)
    factoryOf(::FetchShouldInsertAtTopOnDownloadFlowUseCase)
    factoryOf(::UpdateIsStepEnabledUseCase)
    factoryOf(::UpdatePlaylistIdOnDownloadUseCase)
    factoryOf(::FetchDownloadTargetPlaylistsFlowUseCase)
    factoryOf(::UpdateShouldInsertAtTopOnDownloadUseCase)
    factoryOf(::ExtractAudioWaveformUseCase)
    factoryOf(::ApplyAudioEditUseCase)
    factoryOf(::SaveAudioEditUseCase)
    factoryOf(::FetchAudioEditTimelineUseCase)
    factoryOf(::ObserveAudioEditSavingUseCase)
    factoryOf(::FetchSavingAudioMediaIdsFlowUseCase)
    factoryOf(::ReserveAudioEditSaveUseCase)
    factoryOf(::ReleaseAudioEditSaveUseCase)
    factoryOf(::MeasureAudioDurationUseCase)
    factoryOf(::RevertAudioEditUseCase)
    factoryOf(::SaveEditedAudioAsNewMediaUseCase)

    factoryOf(::OpenMigrationSendUseCase)
    factoryOf(::CloseMigrationSendUseCase)
    factoryOf(::FetchMigrationSendStatusUseCase)
    factoryOf(::ExportMigrationDataUseCase)
    factoryOf(::FetchShareablePlaylistListFlowUseCase)
    factoryOf(::FetchShareableAudioMediaListFlowUseCase)
    factoryOf(::ResolveMigrationFilePathUseCase)
    factoryOf(::PairMigrationSourceUseCase)
    factoryOf(::ImportMigrationDataUseCase)
    factoryOf(::ReceiveMigrationDataUseCase)
    factoryOf(::ClearMigrationStagingUseCase)

    factoryOf(::BackupNowUseCase)
    factoryOf(::BackupIfDueUseCase)
    factoryOf(::RegisterBackupFolderUseCase)
    factoryOf(::FetchBackupStatusFlowUseCase)
    factoryOf(::ReadBackupFileUseCase)
    factoryOf(::FindBackupToRestoreUseCase)
    factoryOf(::HasLocalBackupUseCase)
    factoryOf(::PreviewRestoreUseCase)
    factoryOf(::RestoreBackupUseCase)
    factoryOf(::FetchConnectionStatusUseCase)

    factoryOf(::PreviewPlaylistDownloadUseCase)
    factoryOf(::DownloadYoutubePlaylistUseCase)

    factoryOf(::CleanupAnomalousDataUseCase)
    factoryOf(::DeleteAudioMediaListUseCase)
    factoryOf(::DeleteAudioMediaUseCase)
    factoryOf(::DownloadAudioMediaUseCase)
    factoryOf(::ExportAudioMediaListToDeviceUseCase)
    factoryOf(::FetchAllAudioMediaListFlowUseCase)
    factoryOf(::ImportSharedAudioMediaUseCase)
    factoryOf(::ImportUploadedAudioMediaUseCase)
    factoryOf(::ValidateUploadedAudioFileNameUseCase)
    factoryOf(::CreatePcUploadHandlerUseCase)
    factoryOf(::StartPcFileShareServerUseCase)
    factoryOf(::StopPcFileShareServerUseCase)
    factoryOf(::FetchPcFileShareServerRunningUseCase)
    factoryOf(::SaveAudioMediaImageUseCase)
    factoryOf(::UpdateAudioMediaUseCase)
    factoryOf(::SearchLyricsUseCase)
    factoryOf(::BuildSongSearchQueryUseCase)
    factoryOf(::SortLyricsSearchResultsUseCase)
    factoryOf(::MatchLyricsTrackNameUseCase)
    factoryOf(::ApplyLyricsToAudioMediaUseCase)
    factoryOf(::DownloadLyricsForAudioMediaUseCase)
    factoryOf(::GetAudioMediaLyricsUseCase)
    factoryOf(::DeleteAudioMediaLyricsUseCase)
    factoryOf(::ParseLyricsUseCase)
    factoryOf(::GetParsedLyricsUseCase)
    factoryOf(::SaveEditedLyricsUseCase)
    factoryOf(::SerializeLyricsUseCase)
    factoryOf(::DetectLyricsLanguageUseCase)
    factoryOf(::CreateLyricsVariantUseCase)
    factoryOf(::GetLyricsVariantUseCase)
    factoryOf(::SplitLyricsByNewlineUseCase)
    factoryOf(::CheckAppUpdateUseCase)
    factoryOf(::CheckAppUpdateOncePerDayUseCase)
    factoryOf(::IgnoreAppUpdateUseCase)
    factoryOf(::SearchKaraokeUseCase)
    factoryOf(::ApplyKaraokeNumberToAudioMediaUseCase)
    factoryOf(::DeleteAudioMediaKaraokeNumberUseCase)
    factoryOf(::FetchIsPlaybackRepeatingFlowUseCase)
    factoryOf(::FetchIsPlaybackShufflingFlowUseCase)
    factoryOf(::FetchPlaybackPitchSemitoneFlowUseCase)
    factoryOf(::FetchPlaybackSpeedFlowUseCase)
    factoryOf(::UpdateIsPlaybackShufflingUseCase)
    factoryOf(::UpdatePlaybackPitchSemitoneUseCase)
    factoryOf(::UpdatePlaybackRepeatingUseCase)
    factoryOf(::UpdatePlaybackSpeedUseCase)
    factoryOf(::FetchSleepTimerMinutesFlowUseCase)
    factoryOf(::FetchSleepTimerAnnouncementMediaIdFlowUseCase)
    factoryOf(::FetchSleepTimerAnnouncementOptionsFlowUseCase)
    factoryOf(::UpdateSleepTimerMinutesUseCase)
    factoryOf(::UpdateSleepTimerAnnouncementMediaIdUseCase)
    factoryOf(::AddAudioMediaListToPlaylistUseCase)
    factoryOf(::DeleteAudioTrackListUseCase)
    factoryOf(::ToggleFavoriteUseCase)
    factoryOf(::FetchAudioMediaDetailFlowUseCase)
    factoryOf(::FetchAudioMediaListFlowUseCase)
    factoryOf(::FetchAudioMediaListToAddTrackFlowUseCase)
    factoryOf(::UpdateTrackSequenceUseCase)
    factoryOf(::FetchLoadBalancingDelaySecondUseCase)
    factoryOf(::EnsureSystemPlaylistsUseCase)
    factoryOf(::FetchSecretModeEnabledFlowUseCase)
    factoryOf(::StartSecretModeUseCase)
    factoryOf(::EndSecretModeUseCase)
    factoryOf(::FetchSecretModeMinutesFlowUseCase)
    factoryOf(::FetchSecretAudioMediaIdListFlowUseCase)
    factoryOf(::UpdateSecretModeMinutesUseCase)
    factoryOf(::FetchPlaylistsByTypeUseCase)
    factoryOf(::AddNewPlaylistUseCase)
    factoryOf(::DeletePlaylistUseCase)
    factoryOf(::DeletePlaylistWithTracksUseCase)
    factoryOf(::FetchAllPlaylistFlowUseCase)
    factoryOf(::FetchPlaylistFlowUseCase)
    factoryOf(::UpdatePlaylistDescriptionUseCase)
    factoryOf(::UpdatePlaylistTitleUseCase)
    factoryOf(::UpdatePlaylistImageUseCase)
    factoryOf(::ResetPlaylistImageUseCase)
    factoryOf(::UpdatePlaylistSequenceUseCase)
    factoryOf(::BackupStepSensorUseCase)
    factoryOf(::CalculateStepCountUseCase)
    factoryOf(::GetAccelerateStepUseCase)
    factoryOf(::GetHalfHourlyStepsUseCase)
    factoryOf(::GetLastStepSensorForTodayUseCase)
    factoryOf(::GetMonthlyDailyStepsUseCase)
    factoryOf(::GetStepCountUseCase)
    factoryOf(::ReplaceStepSensorsUseCase)
    factoryOf(::ResetDataOncePerDayUseCase)
    factoryOf(::UpdateAccelerateStepUseCase)
    factoryOf(::UpdateLastStepSensorUseCase)
    factoryOf(::RegisterUserUseCase)
    factoryOf(::FetchUserIdUseCase)
}
