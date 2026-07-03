package com.kintmin.jellytube

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.kintmin.platform.deeplink.DeepLinkConstants
import com.kintmin.presentation.ui.audio_media_detail.navigation.audioMediaDetailScreen
import com.kintmin.presentation.ui.audio_media_detail.navigation.navigateToAudioMediaDetailScreen
import com.kintmin.presentation.ui.audio_media_edit.navigation.audioMediaEdit
import com.kintmin.presentation.ui.audio_media_edit.navigation.navigateToAudioMediaEditScreen
import com.kintmin.presentation.ui.main.MainTabItem
import com.kintmin.presentation.ui.playlist_detail.navigation.navigateToPlaylistDetailScreen
import com.kintmin.presentation.ui.playlist_detail.navigation.playlistDetail
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.main.navigation.mainScreen
import com.kintmin.presentation.ui.main.navigation.navigateToMainScreen
import com.kintmin.presentation.ui.player_detail.navigation.navigateToPlayerDetailScreen
import com.kintmin.presentation.ui.player_detail.navigation.playerDetailScreen
import com.kintmin.presentation.ui.playlist_add.navigation.navigateToPlaylistAddScreen
import com.kintmin.presentation.ui.playlist_add.navigation.playlistAdd
import com.kintmin.presentation.ui.playlist_edit.navigation.navigateToPlaylistEditScreen
import com.kintmin.presentation.ui.playlist_edit.navigation.playlistEdit
import com.kintmin.presentation.ui.setting.app_log.navigation.appLogScreen
import com.kintmin.presentation.ui.setting.app_log.navigation.navigateToAppLogScreen
import com.kintmin.presentation.ui.setting.navigation.navigateToSettingScreen
import com.kintmin.presentation.ui.setting.navigation.settingGraph
import com.kintmin.presentation.ui.setting.file_share_receive.navigation.navigateToSettingFileShareReceiveScreen
import com.kintmin.presentation.ui.setting.file_share_receive.navigation.settingFileShareReceiveScreen
import com.kintmin.presentation.ui.setting.quick_share.navigation.navigateToSettingShareScreen
import com.kintmin.presentation.ui.setting.quick_share.navigation.settingShareScreen
import com.kintmin.presentation.ui.step.navigation.navigateToStepScreen
import com.kintmin.presentation.ui.step.navigation.stepScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

val navOptions = navOptions {
    launchSingleTop = true
    restoreState = true
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    navigationIntentFlow: Flow<NavigationIntent>,
    onDeepLink: (Uri) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = MainScreenRoute(MainTabItem.Playlist),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300),
            )
        },
    ) {
        mainScreen(
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
            navigateToPlaylistAdd = { playlistId ->
                navController.navigateToPlaylistAddScreen(playlistId, navOptions)
            },
            navigateToSetting = {
                navController.navigateToSettingScreen(navOptions)
            },
            navigateToPlayerDetail = {
                navController.navigateToPlayerDetailScreen(navOptions)
            },
            navigateToFileShareReceive = {
                navController.navigateToSettingFileShareReceiveScreen(navOptions)
            },
        )
        playlistDetail(
            navigateToBack = { navController.popBackStack() },
            navigateToAddAudioMediaScreen = { playlistId ->
                navController.navigateToPlaylistAddScreen(playlistId, navOptions)
            },
            navigateToPlaylistEditScreen = { playlistId, focusAudioMediaId ->
                navController.navigateToPlaylistEditScreen(playlistId, focusAudioMediaId, navOptions)
            },
            navigateToAudioDetailScreen = { audioMediaId ->
                navController.navigateToAudioMediaDetailScreen(audioMediaId, navOptions)
            },
            navigateToAudioEditScreen = { audioMediaId ->
                navController.navigateToAudioMediaEditScreen(audioMediaId, navOptions)
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
            navigationToAudioMediaEditScreen = { audioMediaId ->
                navController.navigateToAudioMediaEditScreen(audioMediaId, navOptions)
            },
            navigateToMainSearchTab = { url ->
                navController.navigateToMainScreen(
                    MainTabItem.Search,
                    navOptions,
                    url,
                )
            },
            navigateToPlaylistDetailScreen = { playlistId, audioMediaId ->
                onDeepLink(DeepLinkConstants.UriBuilder.playlistContentScreen(playlistId, audioMediaId))
            },
        )
        audioMediaEdit(
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
            navigateToShare = {
                navController.navigateToSettingShareScreen(navOptions)
            },
            navigateToFileShareReceive = {
                navController.navigateToSettingFileShareReceiveScreen(navOptions)
            },
        ) {
            appLogScreen(
                navigateToBack = { navController.popBackStack() },
            )

            stepScreen(
                navigateToBack = { navController.popBackStack() },
            )

            settingShareScreen(
                navigateToBack = { navController.popBackStack() },
            )

            settingFileShareReceiveScreen(
                navigateToBack = { navController.popBackStack() },
            )
        }

        playerDetailScreen(
            navigateToBack = { navController.popBackStack() },
            navigateToAudioMediaDetail = { audioMediaId ->
                navController.navigateToAudioMediaDetailScreen(audioMediaId, navOptions)
            },
            navigateToAudioMediaEdit = { audioMediaId ->
                navController.navigateToAudioMediaEditScreen(audioMediaId, navOptions)
            },
            navigateToPlayingPlaylist = { playlistId, audioMediaId ->
                onDeepLink(DeepLinkConstants.UriBuilder.playlistContentScreen(playlistId, audioMediaId))
            },
        )
    }

    LaunchedEffect(navController, navigationIntentFlow) {
        navigationIntentFlow.collectLatest { navigationIntent ->
            consumeNavigationIntent(navController, navigationIntent)
        }
    }
}

private fun consumeNavigationIntent(
    navController: NavHostController,
    navigationIntent: NavigationIntent,
) {
    when (navigationIntent) {
        NavigationIntent.PopAll -> {
            navController.navigate(MainScreenRoute(MainTabItem.Playlist)) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
        NavigationIntent.NavigateToSettings -> {
            navController.navigateToSettingScreen(navOptions)
        }
        NavigationIntent.NavigateToSettingAppLog -> {
            navController.navigateToAppLogScreen(navOptions)
        }
        NavigationIntent.NavigateToStep -> {
            navController.navigateToStepScreen(navOptions)
        }
        is NavigationIntent.NavigateToMainDownloadTab -> {
            navController.navigateToMainScreen(
                MainTabItem.Search,
                navOptions,
                navigationIntent.targetUrl,
            )
        }
        NavigationIntent.NavigateToMainPlaylistsTab -> {
            navController.navigateToMainScreen(
                MainTabItem.Playlist,
                navOptions,
            )
        }
        is NavigationIntent.NavigateToPlaylistContent -> {
            navController.navigateToPlaylistDetailScreen(
                navigationIntent.playlistId,
                navigationIntent.focusAudioMediaId,
                navOptions,
            )
        }
        is NavigationIntent.NavigateToAudioMedia -> {
            navController.navigateToAudioMediaDetailScreen(
                navigationIntent.audioMediaId,
                navOptions,
            )
        }
        NavigationIntent.NavigateToPlayer -> {
            navController.navigateToPlayerDetailScreen(navOptions)
        }
    }
}
