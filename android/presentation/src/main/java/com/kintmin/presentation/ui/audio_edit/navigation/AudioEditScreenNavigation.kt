package com.kintmin.presentation.ui.audio_edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.audio_edit.AudioEditScreen
import kotlinx.serialization.Serializable

@Serializable
data class AudioEditScreenRoute(val audioMediaId: Int)

fun NavController.navigateToAudioEditScreen(
    audioMediaId: Int,
    navOptions: NavOptions,
) = navigate(AudioEditScreenRoute(audioMediaId), navOptions)

fun NavGraphBuilder.audioEditScreen(
    navigateToBack: () -> Unit,
) {
    composable<AudioEditScreenRoute> {
        AudioEditScreen(
            navigateToBack = navigateToBack,
        )
    }
}
