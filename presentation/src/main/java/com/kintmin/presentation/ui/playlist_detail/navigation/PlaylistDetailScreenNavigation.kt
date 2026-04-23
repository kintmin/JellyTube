package com.kintmin.presentation.ui.playlist_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.playlist_detail.PlaylistDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDetailScreenRoute(
    val playlistId: Int,
    val focusAudioMediaId: Int? = null,
)

fun NavController.navigateToPlaylistDetailScreen(
    playlistId: Int,
    focusAudioMediaId: Int? = null,
    navOptions: NavOptions,
) = navigate(PlaylistDetailScreenRoute(playlistId, focusAudioMediaId), navOptions)

fun NavGraphBuilder.playlistDetail(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: (playlistId: Int) -> Unit,
    navigateToPlaylistEditScreen: (playlistId: Int) -> Unit,
    navigateToAudioDetailScreen: (audioMediaId: Int) -> Unit,
    navigateToPlayerDetail: () -> Unit,
) {
    composable<PlaylistDetailScreenRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PlaylistDetailScreenRoute>()
        PlaylistDetailScreen(
            navigateToBack = navigateToBack,
            navigateToAddAudioMediaScreen = navigateToAddAudioMediaScreen,
            navigateToPlaylistEditScreen = navigateToPlaylistEditScreen,
            navigateToAudioDetailScreen = navigateToAudioDetailScreen,
            navigateToPlayerDetail = navigateToPlayerDetail,
            focusAudioMediaId = route.focusAudioMediaId,
        )
    }
}
