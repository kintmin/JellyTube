package com.kintmin.presentation.ui.playlist_add.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.playlist_add.PlaylistAddScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistAddScreenRoute(
    val playlistId: Int,
    val playlistNameSeed: String = "",
)

fun NavController.navigateToPlaylistAddScreen(
    playlistId: Int,
    navOptions: NavOptions,
    playlistNameSeed: String = "",
) = navigate(PlaylistAddScreenRoute(playlistId, playlistNameSeed), navOptions)

fun NavGraphBuilder.playlistAdd(
    navigateToBack: () -> Unit,
) {
    composable<PlaylistAddScreenRoute> {
        PlaylistAddScreen(
            navigateToBack = navigateToBack,
        )
    }
}