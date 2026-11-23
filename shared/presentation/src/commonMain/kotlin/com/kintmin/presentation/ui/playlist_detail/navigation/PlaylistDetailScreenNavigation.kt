package com.kintmin.presentation.ui.playlist_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.model.SecretTrackFilter
import com.kintmin.presentation.ui.playlist_detail.PlaylistDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDetailScreenRoute(
    val playlistId: Int,
    val focusAudioMediaId: Int? = null,
    val isBasePlaylist: Boolean = false,
    /** 비밀 화면의 '찜' 입구. 즐겨찾기를 열되 비밀 플레이리스트의 곡만 보여준다. */
    val secretOnly: Boolean = false,
) {
    val secretTrackFilter: SecretTrackFilter
        get() = if (secretOnly) SecretTrackFilter.ONLY else SecretTrackFilter.EXCLUDE
}

fun NavController.navigateToPlaylistDetailScreen(
    playlistId: Int,
    focusAudioMediaId: Int? = null,
    navOptions: NavOptions,
    isBasePlaylist: Boolean = false,
    secretOnly: Boolean = false,
) = navigate(PlaylistDetailScreenRoute(playlistId, focusAudioMediaId, isBasePlaylist, secretOnly), navOptions)

fun NavGraphBuilder.playlistDetail(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: (playlistId: Int, playlistName: String) -> Unit,
    navigateToPlaylistEditScreen: (playlistId: Int, focusAudioMediaId: Int?) -> Unit,
    navigateToAudioDetailScreen: (audioMediaId: Int) -> Unit,
    navigateToAudioEditScreen: (audioMediaId: Int) -> Unit,
    navigateToPlayerDetail: () -> Unit,
) {
    composable<PlaylistDetailScreenRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PlaylistDetailScreenRoute>()
        PlaylistDetailScreen(
            playlistId = route.playlistId,
            navigateToBack = navigateToBack,
            navigateToAddAudioMediaScreen = navigateToAddAudioMediaScreen,
            navigateToPlaylistEditScreen = navigateToPlaylistEditScreen,
            navigateToAudioDetailScreen = navigateToAudioDetailScreen,
            navigateToAudioEditScreen = navigateToAudioEditScreen,
            navigateToPlayerDetail = navigateToPlayerDetail,
            focusAudioMediaId = route.focusAudioMediaId,
        )
    }
}
