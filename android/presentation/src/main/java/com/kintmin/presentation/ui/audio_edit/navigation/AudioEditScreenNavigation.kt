package com.kintmin.presentation.ui.audio_edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.audio_edit.AudioEditScreen
import kotlinx.serialization.Serializable

/**
 * 음원 파형 편집(구간 자르기·이어붙이기) 화면.
 *
 * 이름이 비슷한 `AudioMediaEditScreenRoute`는 제목·아티스트 같은 **음원 정보**를 고치는 화면이다.
 */
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
