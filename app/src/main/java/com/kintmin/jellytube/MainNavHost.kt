package com.kintmin.jellytube

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.kintmin.presentation.ui.audio_media_detail.navigation.audioMediaDetailScreen
import com.kintmin.presentation.ui.audio_media_detail.navigation.navigateToAudioMediaDetailScreen
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

@Composable
fun MainNavHost(
    navController: NavHostController,
) {
    val navOptions = NavOptions.Builder().build()

    NavHost(
        navController = navController,
        startDestination = MainScreenRoute(MainTabItem.Playlist),
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
        )
        playlistDetail(
            navigateToBack = { navController.popBackStack() },
            navigateToAddAudioMediaScreen = { playlistId ->
                navController.navigateToPlaylistAddScreen(playlistId, navOptions)
            },
            navigateToPlaylistEditScreen = { playlistId ->
                navController.navigateToPlaylistEditScreen(playlistId, navOptions)
            },
            navigateToAudioDetailScreen = { playlistId, audioMediaId ->
                navController.navigateToAudioMediaDetailScreen(playlistId, audioMediaId, navOptions)
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
            navigationToAudioMediaEditScreen = {},
        )
    }
}

//navOptions {
//    // Pop up to the start destination of the graph to
//    // avoid building up a large stack of destinations
//    // on the back stack as users select items
//    popUpTo(navController.graph.findStartDestination().id) {
//        saveState = true
//    }
//    // Avoid multiple copies of the same destination when
//    // reselecting the same item
//    launchSingleTop = true
//    // Restore state when reselecting a previously selected item
//    restoreState = true
//}