package com.kintmin.presentation.ui.player_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.player_detail.PlayerDetailScreen
import kotlinx.serialization.Serializable

@Serializable
object PlayerDetailScreenRoute

fun NavController.navigateToPlayerDetailScreen(
    navOptions: NavOptions,
) = navigate(PlayerDetailScreenRoute, navOptions)

fun NavGraphBuilder.playerDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAudioMediaDetail: (audioMediaId: Int) -> Unit,
    navigateToAudioMediaInformationEdit: (audioMediaId: Int) -> Unit,
    navigateToPlayingPlaylist: (playlistId: Int, audioMediaId: Int?) -> Unit,
    navigateToLyricsViewer: (audioMediaId: Int) -> Unit,
) {
    composable<PlayerDetailScreenRoute> {
        PlayerDetailScreen(
            navigateToBack = navigateToBack,
            navigateToAudioMediaDetail = navigateToAudioMediaDetail,
            navigateToAudioMediaInformationEdit = navigateToAudioMediaInformationEdit,
            navigateToPlayingPlaylist = navigateToPlayingPlaylist,
            navigateToLyricsViewer = navigateToLyricsViewer,
        )
    }
}
