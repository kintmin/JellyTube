package com.kintmin.presentation.ui.audio_media_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.audio_media_detail.AudioMediaDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class AudioMediaDetailScreenRoute(val audioMediaId: Int)

fun NavController.navigateToAudioMediaDetailScreen(
    audioMediaId: Int,
    navOptions: NavOptions,
) = navigate(AudioMediaDetailScreenRoute(audioMediaId), navOptions)

fun NavGraphBuilder.audioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaEditScreen: (audioMediaId: Int) -> Unit,
    navigateToMainSearchTab: (url: String) -> Unit,
    navigateToPlaylistDetailScreen: (playlistId: Int, audioMediaId: Int) -> Unit,
) {
    composable<AudioMediaDetailScreenRoute> { backStackEntry ->
        AudioMediaDetailScreen(
            navigateToBack = navigateToBack,
            navigationToAudioMediaEditScreen = navigationToAudioMediaEditScreen,
            navigateToMainSearchTab = navigateToMainSearchTab,
            navigateToPlaylistDetailScreen = navigateToPlaylistDetailScreen,
        )
    }
}