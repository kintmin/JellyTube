package com.kintmin.presentation.ui.playlist_edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.playlist_edit.PlaylistEditScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistEditScreenRoute(
    val playlistId: Int,
    val focusAudioMediaId: Int? = null,
)

fun NavController.navigateToPlaylistEditScreen(
    playlistId: Int,
    focusAudioMediaId: Int? = null,
    navOptions: NavOptions,
) = navigate(PlaylistEditScreenRoute(playlistId, focusAudioMediaId), navOptions)

fun NavGraphBuilder.playlistEdit(navigateToBack: () -> Unit) {
    composable<PlaylistEditScreenRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PlaylistEditScreenRoute>()
        PlaylistEditScreen(
            navigateToBack = navigateToBack,
            focusAudioMediaId = route.focusAudioMediaId,
        )
    }
}
