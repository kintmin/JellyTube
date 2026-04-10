package com.kintmin.jellytube

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
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
import com.kintmin.presentation.ui.playlist_add.navigation.navigateToPlaylistAddScreen
import com.kintmin.presentation.ui.playlist_add.navigation.playlistAdd
import com.kintmin.presentation.ui.playlist_edit.navigation.navigateToPlaylistEditScreen
import com.kintmin.presentation.ui.playlist_edit.navigation.playlistEdit
import com.kintmin.presentation.ui.setting.navigation.navigateToSettingScreen
import com.kintmin.presentation.ui.setting.navigation.settingScreen

@Composable
fun MainNavHost(
    navController: NavHostController,
) {
    val navOptions = navOptions {
        launchSingleTop = true
    }

    NavHost(
        navController = navController,
        startDestination = MainScreenRoute(MainTabItem.Playlist),
    ) {
        mainScreen(
            navigateToPlaylistDetail = { playlistId ->
                navController.navigateToMainScreen(MainTabItem.Playlist, navOptions)
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
        settingScreen(
            navigateToBack = { navController.popBackStack() },
        )
    }
}

fun getDeepNav(navController: NavHostController) = navOptions {
    popUpTo(navController.graph.findStartDestination().id) {
        inclusive = true
    }
    launchSingleTop = true
    restoreState = true
}
