package com.kintmin.presentation.ui.playlist_download.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.playlist_download.PlaylistDownloadScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDownloadScreenRoute(val url: String)

fun NavController.navigateToPlaylistDownloadScreen(
    url: String,
    navOptions: NavOptions,
) = navigate(PlaylistDownloadScreenRoute(url), navOptions)

fun NavGraphBuilder.playlistDownloadScreen(
    navigateToBack: () -> Unit,
    navigateToPlaylist: (playlistId: Int) -> Unit,
) {
    composable<PlaylistDownloadScreenRoute> {
        PlaylistDownloadScreen(
            navigateToBack = navigateToBack,
            navigateToPlaylist = navigateToPlaylist,
        )
    }
}
