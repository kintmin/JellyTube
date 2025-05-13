package com.kintmin.presentation.ui.audio_media_edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.audio_media_edit.AudioMediaEditScreen
import kotlinx.serialization.Serializable

@Serializable
data class AudioMediaEditScreenRoute(val audioMediaId: Int)

fun NavController.navigateToAudioMediaEditScreen(
    audioMediaId: Int,
    navOptions: NavOptions,
) = navigate(AudioMediaEditScreenRoute(audioMediaId), navOptions)

fun NavGraphBuilder.audioMediaEdit(
    navigateToBack: () -> Unit,
) {
    composable<AudioMediaEditScreenRoute> {
        AudioMediaEditScreen(
            navigateToBack = navigateToBack,
        )
    }
}