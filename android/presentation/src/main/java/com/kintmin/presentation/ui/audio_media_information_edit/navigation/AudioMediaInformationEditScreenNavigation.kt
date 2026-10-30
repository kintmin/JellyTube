package com.kintmin.presentation.ui.audio_media_information_edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.audio_media_information_edit.AudioMediaInformationEditScreen
import kotlinx.serialization.Serializable

@Serializable
data class AudioMediaInformationEditScreenRoute(val audioMediaId: Int)

fun NavController.navigateToAudioMediaInformationEditScreen(
    audioMediaId: Int,
    navOptions: NavOptions,
) = navigate(AudioMediaInformationEditScreenRoute(audioMediaId), navOptions)

fun NavGraphBuilder.audioMediaInformationEdit(
    navigateToBack: () -> Unit,
) {
    composable<AudioMediaInformationEditScreenRoute> {
        AudioMediaInformationEditScreen(
            navigateToBack = navigateToBack,
        )
    }
}