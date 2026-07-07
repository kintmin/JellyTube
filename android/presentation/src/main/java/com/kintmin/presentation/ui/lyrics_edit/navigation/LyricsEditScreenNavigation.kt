package com.kintmin.presentation.ui.lyrics_edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.lyrics_edit.LyricsEditScreen
import kotlinx.serialization.Serializable

@Serializable
data class LyricsEditScreenRoute(val audioMediaId: Int)

fun NavController.navigateToLyricsEditScreen(
    audioMediaId: Int,
    navOptions: NavOptions,
) = navigate(LyricsEditScreenRoute(audioMediaId), navOptions)

fun NavGraphBuilder.lyricsEditScreen(
    navigateToBack: () -> Unit,
) {
    composable<LyricsEditScreenRoute> {
        LyricsEditScreen(
            navigateToBack = navigateToBack,
        )
    }
}
