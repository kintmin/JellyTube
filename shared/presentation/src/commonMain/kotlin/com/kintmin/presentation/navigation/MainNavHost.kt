package com.kintmin.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.kintmin.presentation.deeplink.DeepLinkConstants
import com.kintmin.presentation.ui.audio_edit.navigation.audioEditScreen
import com.kintmin.presentation.ui.audio_edit.navigation.navigateToAudioEditScreen
import com.kintmin.presentation.ui.audio_media_detail.navigation.audioMediaDetailScreen
import com.kintmin.presentation.ui.audio_media_detail.navigation.navigateToAudioMediaDetailScreen
import com.kintmin.presentation.ui.audio_media_information_edit.navigation.audioMediaInformationEdit
import com.kintmin.presentation.ui.audio_media_information_edit.navigation.navigateToAudioMediaInformationEditScreen
import com.kintmin.presentation.ui.karaoke_search.navigation.karaokeSearchScreen
import com.kintmin.presentation.ui.karaoke_search.navigation.navigateToKaraokeSearchScreen
import com.kintmin.presentation.ui.lyrics_detail.navigation.lyricsDetailScreen
import com.kintmin.presentation.ui.lyrics_detail.navigation.navigateToLyricsDetailScreen
import com.kintmin.presentation.ui.lyrics_edit.navigation.lyricsEditScreen
import com.kintmin.presentation.ui.lyrics_edit.navigation.navigateToLyricsEditScreen
import com.kintmin.presentation.ui.lyrics_search.navigation.lyricsSearchScreen
import com.kintmin.presentation.ui.lyrics_search.navigation.navigateToLyricsSearchScreen
import com.kintmin.presentation.ui.lyrics_viewer.navigation.lyricsViewerScreen
import com.kintmin.presentation.ui.lyrics_viewer.navigation.navigateToLyricsViewerScreen
import com.kintmin.presentation.ui.main.MainTabItem
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.main.navigation.mainScreen
import com.kintmin.presentation.ui.main.navigation.navigateToMainScreen
import com.kintmin.presentation.ui.player_detail.navigation.navigateToPlayerDetailScreen
import com.kintmin.presentation.ui.player_detail.navigation.playerDetailScreen
import com.kintmin.presentation.ui.playlist_add.navigation.navigateToPlaylistAddScreen
import com.kintmin.presentation.ui.playlist_add.navigation.playlistAdd
import com.kintmin.presentation.ui.playlist_detail.navigation.navigateToPlaylistDetailScreen
import com.kintmin.presentation.ui.playlist_detail.navigation.playlistDetail
import com.kintmin.presentation.ui.playlist_download.navigation.navigateToPlaylistDownloadScreen
import com.kintmin.presentation.ui.playlist_download.navigation.playlistDownloadScreen
import com.kintmin.presentation.ui.playlist_edit.navigation.navigateToPlaylistEditScreen
import com.kintmin.presentation.ui.playlist_edit.navigation.playlistEdit
import com.kintmin.presentation.ui.secret_playlist.navigation.navigateToSecretPlaylistScreen
import com.kintmin.presentation.ui.secret_playlist.navigation.secretPlaylistScreen
import com.kintmin.presentation.ui.setting.app_log.navigation.appLogScreen
import com.kintmin.presentation.ui.setting.app_log.navigation.navigateToAppLogScreen
import com.kintmin.presentation.ui.setting.backup_restore.navigation.navigateToSettingBackupRestoreScreen
import com.kintmin.presentation.ui.setting.backup_restore.navigation.settingBackupRestoreScreen
import com.kintmin.presentation.ui.setting.data_migration_receive.navigation.navigateToSettingDataMigrationReceiveScreen
import com.kintmin.presentation.ui.setting.data_migration_receive.navigation.settingDataMigrationReceiveScreen
import com.kintmin.presentation.ui.setting.data_migration_send.navigation.navigateToSettingDataMigrationSendScreen
import com.kintmin.presentation.ui.setting.data_migration_send.navigation.settingDataMigrationSendScreen
import com.kintmin.presentation.ui.setting.data_migration_share.MigrationShareKind
import com.kintmin.presentation.ui.setting.data_migration_share.navigation.navigateToSettingDataMigrationShareScreen
import com.kintmin.presentation.ui.setting.data_migration_share.navigation.settingDataMigrationShareScreen
import com.kintmin.presentation.ui.setting.data_migration_share_audio.navigation.navigateToSettingDataMigrationShareAudioScreen
import com.kintmin.presentation.ui.setting.data_migration_share_audio.navigation.settingDataMigrationShareAudioScreen
import com.kintmin.presentation.ui.setting.data_migration_share_playlist.navigation.navigateToSettingDataMigrationSharePlaylistScreen
import com.kintmin.presentation.ui.setting.data_migration_share_playlist.navigation.settingDataMigrationSharePlaylistScreen
import com.kintmin.presentation.ui.setting.device_save.navigation.navigateToSettingDeviceSaveScreen
import com.kintmin.presentation.ui.setting.device_save.navigation.settingDeviceSaveScreen
import com.kintmin.presentation.ui.setting.device_save_select.navigation.navigateToSettingDeviceSaveSelectScreen
import com.kintmin.presentation.ui.setting.device_save_select.navigation.settingDeviceSaveSelectScreen
import com.kintmin.presentation.ui.setting.file_share_receive.navigation.navigateToSettingFileShareReceiveScreen
import com.kintmin.presentation.ui.setting.file_share_receive.navigation.settingFileShareReceiveScreen
import com.kintmin.presentation.ui.setting.navigation.navigateToSettingScreen
import com.kintmin.presentation.ui.setting.secret_playlist_guide.navigation.navigateToSettingSecretPlaylistGuideScreen
import com.kintmin.presentation.ui.setting.secret_playlist_guide.navigation.settingSecretPlaylistGuideScreen
import com.kintmin.presentation.ui.setting.navigation.settingGraph
import com.kintmin.presentation.ui.step.navigation.navigateToStepScreen
import com.kintmin.presentation.ui.step.navigation.stepScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

val navOptions = navOptions {
    launchSingleTop = true
    restoreState = true
}

private val ScreenSlideSpec = tween<IntOffset>(300)

@Composable
fun MainNavHost(
    navController: NavHostController,
    navigationEventFlow: Flow<NavigationEvent>,
    isDebugTabVisible: Boolean,
    onDeepLink: (url: String) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = MainScreenRoute(MainTabItem.Playlist),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = ScreenSlideSpec,
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = ScreenSlideSpec,
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = ScreenSlideSpec,
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = ScreenSlideSpec,
            )
        },
    ) {
        mainScreen(
            isDebugTabVisible = isDebugTabVisible,
            navigateToPlaylistDetail = { playlistId, isBasePlaylist ->
                navController.navigateToPlaylistDetailScreen(
                    playlistId = playlistId,
                    navOptions = navOptions,
                    isBasePlaylist = isBasePlaylist,
                )
            },
            navigateToPlaylistEdit = { playlistId ->
                navController.navigateToPlaylistEditScreen(playlistId, null, navOptions)
            },
            navigateToPlaylistAdd = { playlistId, playlistName ->
                navController.navigateToPlaylistAddScreen(playlistId, navOptions, playlistName)
            },
            navigateToSetting = {
                navController.navigateToSettingScreen(navOptions)
            },
            navigateToSecretPlaylist = {
                navController.navigateToSecretPlaylistScreen(navOptions)
            },
            navigateToPlayerDetail = {
                navController.navigateToPlayerDetailScreen(navOptions)
            },
            navigateToPlaylistDownload = { url ->
                navController.navigateToPlaylistDownloadScreen(url, navOptions)
            },
        )
        playlistDownloadScreen(
            navigateToBack = { navController.popBackStack() },
            navigateToPlaylist = { playlistId ->
                navController.popBackStack()
                navController.navigateToPlaylistDetailScreen(playlistId = playlistId, navOptions = navOptions)
            },
        )
        secretPlaylistScreen(
            navigateToBack = { navController.popBackStack() },
            navigateToPlaylistDetail = { playlistId ->
                navController.navigateToPlaylistDetailScreen(
                    playlistId = playlistId,
                    navOptions = navOptions,
                    isBasePlaylist = false,
                )
            },
            navigateToPlaylistEdit = { playlistId ->
                navController.navigateToPlaylistEditScreen(playlistId, null, navOptions)
            },
            navigateToPlaylistAdd = { playlistId, playlistName ->
                navController.navigateToPlaylistAddScreen(playlistId, navOptions, playlistName)
            },
            navigateToPlayerDetail = {
                navController.navigateToPlayerDetailScreen(navOptions)
            },
        )
        playlistDetail(
            navigateToBack = { navController.popBackStack() },
            navigateToAddAudioMediaScreen = { playlistId, playlistName ->
                navController.navigateToPlaylistAddScreen(playlistId, navOptions, playlistName)
            },
            navigateToPlaylistEditScreen = { playlistId, focusAudioMediaId ->
                navController.navigateToPlaylistEditScreen(playlistId, focusAudioMediaId, navOptions)
            },
            navigateToAudioDetailScreen = { audioMediaId ->
                navController.navigateToAudioMediaDetailScreen(audioMediaId, navOptions)
            },
            navigateToAudioEditScreen = { audioMediaId ->
                navController.navigateToAudioMediaInformationEditScreen(audioMediaId, navOptions)
            },
            navigateToPlayerDetail = {
                navController.navigateToPlayerDetailScreen(navOptions)
            },
        )
        playlistEdit(
            navigateToBack = { navController.popBackStack() }
        )
        playlistAdd(
            navigateToBack = { navController.popBackStack() }
        )
        audioMediaDetailScreen(
            navigateToBack = { navController.popBackStack() },
            navigationToAudioMediaInformationEditScreen = { audioMediaId ->
                navController.navigateToAudioMediaInformationEditScreen(audioMediaId, navOptions)
            },
            navigateToMainSearchTab = { url ->
                navController.navigateToMainScreen(
                    MainTabItem.Search,
                    navOptions,
                    url,
                )
            },
            navigateToPlaylistDetailScreen = { playlistId, audioMediaId ->
                onDeepLink(DeepLinkConstants.UrlBuilder.playlistContentScreen(playlistId, audioMediaId))
            },
            navigateToLyricsSearch = { audioMediaId, query, durationSeconds ->
                navController.navigateToLyricsSearchScreen(audioMediaId, query, durationSeconds, navOptions)
            },
            navigateToLyricsViewer = { audioMediaId ->
                navController.navigateToLyricsViewerScreen(audioMediaId, navOptions)
            },
            navigateToKaraokeSearch = { audioMediaId, query ->
                navController.navigateToKaraokeSearchScreen(audioMediaId, query, navOptions)
            },
            navigateToAudioEdit = { audioMediaId ->
                navController.navigateToAudioEditScreen(audioMediaId, navOptions)
            },
        )
        audioMediaInformationEdit(
            navigateToBack = { navController.popBackStack() },
        )
        audioEditScreen(
            navigateToBack = { navController.popBackStack() },
        )
        lyricsSearchScreen(
            navigateToBack = { navController.popBackStack() },
            navigateToLyricsEdit = { audioMediaId ->
                navController.navigateToLyricsEditScreen(audioMediaId, navOptions)
            },
            navigateToLyricsDetail = { audioMediaId, trackName, artistName, plainLyrics, syncedLyrics ->
                navController.navigateToLyricsDetailScreen(
                    audioMediaId = audioMediaId,
                    trackName = trackName,
                    artistName = artistName,
                    plainLyrics = plainLyrics,
                    syncedLyrics = syncedLyrics,
                    navOptions = navOptions,
                )
            },
        )
        lyricsDetailScreen(
            navigateToBack = { navController.popBackStack() },
        )
        karaokeSearchScreen(
            navigateToBack = { navController.popBackStack() },
        )

        settingGraph(
            navigateToBack = { navController.popBackStack() },
            navigateToStep = {
                navController.navigateToStepScreen(navOptions)
            },
            navigateToAppLog = {
                navController.navigateToAppLogScreen(navOptions)
            },
            navigateToFileShareReceive = {
                navController.navigateToSettingFileShareReceiveScreen(navOptions)
            },
            navigateToDataMigrationSend = {
                navController.navigateToSettingDataMigrationShareScreen(navOptions)
            },
            navigateToDataMigrationReceive = {
                navController.navigateToSettingDataMigrationReceiveScreen(navOptions)
            },
            navigateToDeviceSave = {
                navController.navigateToSettingDeviceSaveScreen(navOptions)
            },
            navigateToSecretPlaylistGuide = {
                navController.navigateToSettingSecretPlaylistGuideScreen(navOptions)
            },
            navigateToBackupRestore = {
                navController.navigateToSettingBackupRestoreScreen(navOptions)
            },
        ) {
            appLogScreen(
                navigateToBack = { navController.popBackStack() },
            )

            stepScreen(
                navigateToBack = { navController.popBackStack() },
            )

            settingFileShareReceiveScreen(
                navigateToBack = { navController.popBackStack() },
            )

            settingDataMigrationShareScreen(
                navigateToBack = { navController.popBackStack() },
                navigateToSync = {
                    navController.navigateToSettingDataMigrationSendScreen(MigrationShareKind.SYNC, navOptions)
                },
                navigateToPlaylistSelect = {
                    navController.navigateToSettingDataMigrationSharePlaylistScreen(navOptions)
                },
                navigateToAudioMediaSelect = {
                    navController.navigateToSettingDataMigrationShareAudioScreen(navOptions)
                },
            )

            settingDataMigrationSharePlaylistScreen(
                navigateToBack = { navController.popBackStack() },
                navigateToSend = { playlistIdList ->
                    navController.navigateToSettingDataMigrationSendScreen(
                        kind = MigrationShareKind.PLAYLIST,
                        navOptions = navOptions,
                        idList = playlistIdList,
                    )
                },
            )

            settingDataMigrationShareAudioScreen(
                navigateToBack = { navController.popBackStack() },
                navigateToSend = { audioMediaIdList ->
                    navController.navigateToSettingDataMigrationSendScreen(
                        kind = MigrationShareKind.AUDIO_MEDIA,
                        navOptions = navOptions,
                        idList = audioMediaIdList,
                    )
                },
            )

            settingDataMigrationSendScreen(
                navigateToBack = { navController.popBackStack() },
            )

            settingDataMigrationReceiveScreen(
                navigateToBack = { navController.popBackStack() },
            )

            settingBackupRestoreScreen(
                navigateToBack = { navController.popBackStack() },
            )

            settingDeviceSaveScreen(
                navigateToBack = { navController.popBackStack() },
                navigateToDeviceSaveSelect = {
                    navController.navigateToSettingDeviceSaveSelectScreen(navOptions)
                },
            )

            settingSecretPlaylistGuideScreen(
                navigateToBack = { navController.popBackStack() },
                navigateToTryEntry = {
                    onDeepLink(DeepLinkConstants.UrlBuilder.secretPlaylistEntryHint())
                },
            )

            settingDeviceSaveSelectScreen(
                navigateToBack = { navController.popBackStack() },
                navigateToAudioEdit = { audioMediaId ->
                    navController.navigateToAudioEditScreen(audioMediaId, navOptions)
                },
            )
        }

        playerDetailScreen(
            navigateToBack = { navController.popBackStack() },
            navigateToAudioMediaDetail = { audioMediaId ->
                navController.navigateToAudioMediaDetailScreen(audioMediaId, navOptions)
            },
            navigateToAudioMediaInformationEdit = { audioMediaId ->
                navController.navigateToAudioMediaInformationEditScreen(audioMediaId, navOptions)
            },
            navigateToPlayingPlaylist = { playlistId, audioMediaId ->
                onDeepLink(DeepLinkConstants.UrlBuilder.playlistContentScreen(playlistId, audioMediaId))
            },
            navigateToLyricsSearch = { audioMediaId, query, durationSeconds ->
                navController.navigateToLyricsSearchScreen(audioMediaId, query, durationSeconds, navOptions)
            },
            navigateToLyricsViewer = { audioMediaId ->
                navController.navigateToLyricsViewerScreen(audioMediaId, navOptions)
            },
        )
        lyricsViewerScreen(
            navigateToBack = { navController.popBackStack() },
            navigateToLyricsEdit = { audioMediaId ->
                navController.navigateToLyricsEditScreen(audioMediaId, navOptions)
            },
            navigateToPlayerDetail = {
                navController.navigateToPlayerDetailScreen(navOptions)
            },
        )
        lyricsEditScreen(
            navigateToBack = { navController.popBackStack() },
        )
    }

    LaunchedEffect(navController, navigationEventFlow) {
        navigationEventFlow.collectLatest { navigationEvent ->
            consumeNavigationEvent(navController, navigationEvent)
        }
    }
}

private fun consumeNavigationEvent(
    navController: NavHostController,
    navigationEvent: NavigationEvent,
) {
    when (navigationEvent) {
        NavigationEvent.PopAll -> {
            navController.navigate(MainScreenRoute(MainTabItem.Playlist)) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
        NavigationEvent.NavigateToSettings -> {
            navController.navigateToSettingScreen(navOptions)
        }
        NavigationEvent.NavigateToSettingAppLog -> {
            navController.navigateToAppLogScreen(navOptions)
        }
        NavigationEvent.NavigateToStep -> {
            navController.navigateToStepScreen(navOptions)
        }
        is NavigationEvent.NavigateToMainDownloadTab -> {
            navController.navigateToMainScreen(
                MainTabItem.Search,
                navOptions,
                navigationEvent.targetUrl,
            )
        }
        NavigationEvent.HintSecretPlaylistEntry -> {
            // 새 항목이라야 인자를 읽는다. 같은 자리를 다시 쓰면 이전 인자가 남는다.
            navController.navigate(MainScreenRoute(MainTabItem.Playlist, hintSecretPlaylistEntry = true)) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        NavigationEvent.NavigateToMainPlaylistsTab -> {
            navController.navigateToMainScreen(
                MainTabItem.Playlist,
                navOptions,
            )
        }
        is NavigationEvent.NavigateToPlaylistContent -> {
            navController.navigateToPlaylistDetailScreen(
                navigationEvent.playlistId,
                navigationEvent.focusAudioMediaId,
                navOptions,
            )
        }
        is NavigationEvent.NavigateToAudioMedia -> {
            navController.navigateToAudioMediaDetailScreen(
                navigationEvent.audioMediaId,
                navOptions,
            )
        }
        is NavigationEvent.NavigateToLyricsViewer -> {
            navController.navigateToLyricsViewerScreen(
                navigationEvent.audioMediaId,
                navOptions,
            )
        }
        NavigationEvent.NavigateToPlayer -> {
            navController.navigateToPlayerDetailScreen(navOptions)
        }
        NavigationEvent.LeaveSecretScreens -> {
            // 스택 전체를 보는 API 가 공개돼 있지 않아 가려진 비밀 화면을 골라낼 수 없다. 통째로 비운다.
            // 메인 하나뿐이면 걷어 낼 것이 없다 — 앱이 뜰 때마다 메인을 다시 만들지 않게 한다.
            if (navController.previousBackStackEntry == null) return
            navController.navigate(MainScreenRoute(MainTabItem.Playlist)) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }
}

