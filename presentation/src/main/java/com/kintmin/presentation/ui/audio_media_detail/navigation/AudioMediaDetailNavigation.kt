package com.kintmin.presentation.ui.audio_media_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.audio_media_detail.AudioMediaDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class AudioMediaDetailScreenRoute(val playlistId: Int, val audioMediaId: Int)

fun NavController.navigateToAudioMediaDetailScreen(
    playlistId: Int,
    audioMediaId: Int,
    navOptions: NavOptions,
) = navigate(AudioMediaDetailScreenRoute(playlistId, audioMediaId), navOptions)

fun NavGraphBuilder.audioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaEditScreen: () -> Unit,
) {
    composable<AudioMediaDetailScreenRoute> { backStackEntry ->
        AudioMediaDetailScreen(
            navigateToBack = navigateToBack,
            navigationToAudioMediaEditScreen = navigationToAudioMediaEditScreen,
        )
    }
}