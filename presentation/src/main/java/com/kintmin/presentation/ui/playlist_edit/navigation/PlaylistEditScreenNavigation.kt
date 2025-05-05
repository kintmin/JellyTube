package com.kintmin.presentation.ui.playlist_edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.playlist_edit.PlaylistEditScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistEditScreenRoute(val playlistId: Int)

fun NavController.navigateToPlaylistEditScreen(
    playlistId: Int,
    navOptions: NavOptions,
) = navigate(PlaylistEditScreenRoute(playlistId), navOptions)

fun NavGraphBuilder.playlistEdit(navigateToBack: () -> Unit) {
    composable<PlaylistEditScreenRoute> {
        PlaylistEditScreen(
            navigateToBack = navigateToBack,
        )
    }
}