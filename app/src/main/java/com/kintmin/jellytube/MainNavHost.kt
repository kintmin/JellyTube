package com.kintmin.jellytube

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainNavHost(
    navController: NavHostController,
    deepLinkFlow: Flow<Uri>,
) {
    val navOptions = navOptions {
        launchSingleTop = true
    }

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
            navigateToPlaylistDetail = { playlistId ->
                navController.navigateToPlaylistDetailScreen(playlistId, navOptions)
            },
            navigateToPlaylistEdit = { playlistId ->
                navController.navigateToPlaylistEditScreen(playlistId, navOptions)
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
        )
        playlistDetail(
            navigateToBack = { navController.popBackStack() },
            navigateToAddAudioMediaScreen = { playlistId ->
                navController.navigateToPlaylistAddScreen(playlistId, navOptions)
            },
            navigateToPlaylistEditScreen = { playlistId ->
                navController.navigateToPlaylistEditScreen(playlistId, navOptions)
            },
            navigateToAudioDetailScreen = { audioMediaId ->
                navController.navigateToAudioMediaDetailScreen(audioMediaId, navOptions)
            }
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
                    defaultNav(navController),
                    url,
                )
            },
            navigateToPlaylistDetailScreen = { playlistId ->
                navController.navigateToPlaylistDetailScreen(playlistId, defaultNav(navController))
            },
        )
        audioMediaEdit(
            navigateToBack = { navController.popBackStack() },
        )

        settingGraph(
            navigateToBack = { navController.popBackStack() },
            navigateToAppLog = {
                navController.navigateToAppLogScreen(navOptions)
            },
        ) {
            appLogScreen(
                navigateToBack = { navController.popBackStack() },
            )
        }

        playerDetailScreen(
            navigateToBack = { navController.popBackStack() },
        )
    }

    LaunchedEffect(navController, deepLinkFlow) {
        deepLinkFlow.collectLatest { uri ->
            handleDeepLink(navController, uri)
        }
    }
}

fun defaultNav(navController: NavHostController) = navOptions {
    launchSingleTop = true
    restoreState = true
}

private fun handleDeepLink(
    navController: NavHostController,
    deepLink: Uri,
) {
    if (deepLink.scheme != DeepLinkConstants.DEEP_LINK_SCHEME) return
    if (deepLink.host != DeepLinkConstants.DEEP_LINK_HOST) return

    val pathList = deepLink.pathSegments

    val rootPath = pathList.getOrNull(0)
    if (rootPath != DeepLinkConstants.Path.MAIN) return


    navController.navigate(MainScreenRoute(MainTabItem.Playlist)) {
        popUpTo(navController.graph.startDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }

    val mainPath = pathList.getOrNull(1)
    when (mainPath) {
        DeepLinkConstants.Path.SETTINGS -> {    // https://www.jellytube.app.com/main/settings
            navController.navigateToMainScreen(
                tabItem = MainTabItem.Playlist,
                navOptions = defaultNav(navController),
            )
            navController.navigateToSettingScreen(defaultNav(navController))

            val settingsPath = pathList.getOrNull(2)
            when (settingsPath) {
                DeepLinkConstants.Path.APP_LOG -> {     // https://www.jellytube.app.com/main/settings/appLog
                    navController.navigateToAppLogScreen(defaultNav(navController))
                }
            }
        }
        DeepLinkConstants.Path.DOWNLOAD -> {    // https://www.jellytube.app.com/main/download
            val encodedUrl = deepLink.getQueryParameter(DeepLinkConstants.QueryKey.ENCODED_URL)
            navController.navigateToMainScreen(
                tabItem = MainTabItem.Search,
                navOptions = defaultNav(navController),
                searchUrl = encodedUrl,
            )
        }
        DeepLinkConstants.Path.PLAYER -> {   // https://www.jellytube.app.com/main/player
            navController.navigateToMainScreen(
                tabItem = MainTabItem.Playlist,
                navOptions = defaultNav(navController),
            )
            navController.navigateToPlayerDetailScreen(
                navOptions = defaultNav(navController),
            )
        }
        DeepLinkConstants.Path.PLAYLISTS -> {  // https://www.jellytube.app.com/main/playlsts
            navController.navigateToMainScreen(
                tabItem = MainTabItem.Playlist,
                navOptions = defaultNav(navController),
            )

            val playlistPath = pathList.getOrNull(2)?.toIntOrNull()
            if (playlistPath != null) {     // https://www.jellytube.app.com/main/playlsts/{playlistId}
                navController.navigateToPlaylistDetailScreen(
                    playlistId = playlistPath,
                    navOptions = defaultNav(navController),
                )

                val playlistIdPath = pathList.getOrNull(3)
                if (playlistIdPath != DeepLinkConstants.Path.AUDIO_MEDIAS) return

                val focusAudioMediaId = deepLink.getQueryParameter(DeepLinkConstants.QueryKey.FOCUS_AUDIO_MEDIA_ID)
                if (focusAudioMediaId != null) {
                    // TODO: 리스트 스크롤 추가
                }

                val audioMediasPath = pathList.getOrNull(4)?.toIntOrNull()
                if (audioMediasPath != null) {  // https://www.jellytube.app.com/main/playlsts/{playlistId}/audioMedias/{audioMediaId}
                    navController.navigateToAudioMediaDetailScreen(
                        audioMediaId = audioMediasPath,
                        navOptions = defaultNav(navController),
                    )
                }
            }
        }
    }
}
