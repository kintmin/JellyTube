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
                    getDeepNav(navController),
                    url,
                )
            },
            navigateToPlaylistDetailScreen = { playlistId ->
                navController.navigateToPlaylistDetailScreen(playlistId, getDeepNav(navController))
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

fun getDeepNav(navController: NavHostController) = navOptions {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
        inclusive = true
    }
    launchSingleTop = true
    restoreState = true
}

private fun handleDeepLink(
    navController: NavHostController,
    deepLink: Uri
) {
    with(deepLink) {
        if (scheme != DeepLinkConstants.DEEP_LINK_SCHEME) return
        if (host != DeepLinkConstants.DEEP_LINK_HOST) return

        val pathList = pathSegments

        val rootPath = pathList.getOrNull(0)
        when (rootPath) {

            DeepLinkConstants.Path.DOWNLOAD -> {    // https://www.jellytube.app.com/download
                val encodedUrl = getQueryParameter(DeepLinkConstants.QueryKey.ENCODED_URL)
                navController.navigateToMainScreen(
                    tabItem = MainTabItem.Search,
                    navOptions = getDeepNav(navController),
                    searchUrl = encodedUrl,
                )
            }
            DeepLinkConstants.Path.PLAYLIST, null -> {  // https://www.jellytube.app.com/playlst
                navController.navigateToMainScreen(
                    tabItem = MainTabItem.Playlist,
                    navOptions = getDeepNav(navController),
                )

                val playlistPath = pathList.getOrNull(1)
                when {
                    playlistPath == DeepLinkConstants.Path.SETTING -> {     // https://www.jellytube.app.com/playlst/setting
                        navController.navigateToSettingScreen(getDeepNav(navController))

                        val settingPath = pathList.getOrNull(2)
                        when(settingPath) {
                            DeepLinkConstants.Path.APP_LOG -> {     // https://www.jellytube.app.com/playlst/setting/appLog
                                navController.navigateToAppLogScreen(getDeepNav(navController))
                            }
                            else -> null
                        }
                    }
                    playlistPath == DeepLinkConstants.Path.PLAYER -> {      // https://www.jellytube.app.com/playlst/player
                        navController.navigateToPlayerDetailScreen(getDeepNav(navController))
                    }
                    playlistPath?.toIntOrNull() != null -> {    // https://www.jellytube.app.com/playlst/{playlistId}
                        val playlistId = playlistPath.toInt()
                        // TODO: 리스트 스크롤 추가

                        val playlistIdPath = pathList.getOrNull(2)
                        when(playlistIdPath) {
                            DeepLinkConstants.Path.DETAIL -> {      // https://www.jellytube.app.com/playlst/{playlistId}/detail
                                val audioMediaId = pathList.getOrNull(3)?.toIntOrNull()

                                if (audioMediaId == null) {     // https://www.jellytube.app.com/playlst/{playlistId}/detail
                                    navController.navigateToPlaylistDetailScreen(
                                        playlistId = playlistId,
                                        navOptions = getDeepNav(navController),
                                    )
                                } else {    // https://www.jellytube.app.com/playlst/{playlistId}/detail/{audioMediaId}
                                    // TODO: 리스트 스크롤 추가
                                    navController.navigateToPlaylistDetailScreen(
                                        playlistId = playlistId,
                                        navOptions = getDeepNav(navController),
                                    )

                                    val audioMediaIdPath = pathList.getOrNull(4)
                                    when(audioMediaIdPath) {
                                        DeepLinkConstants.Path.AUDIO_MEDIA -> {     // https://www.jellytube.app.com/playlst/{playlistId}/detail/{audioMediaId}/audioMedia
                                            navController.navigateToAudioMediaDetailScreen(
                                                audioMediaId = audioMediaId,
                                                navOptions = getDeepNav(navController),
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }
            }
            else -> null
        }


    }
}
