package com.kintmin.presentation.ui.audio_play.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.audio_play.PlaylistDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDetailScreenRoute(val playlistId: Int)

fun NavController.navigateToPlaylistDetailScreen(
    playlistId: Int,
    navOptions: NavOptions,
) = navigate(PlaylistDetailScreenRoute(playlistId), navOptions)

fun NavGraphBuilder.playlistDetail(navigateToBack: () -> Unit) {
    composable<PlaylistDetailScreenRoute> {
        PlaylistDetailScreen(
            navigateToBack = navigateToBack,
        )
    }
}
