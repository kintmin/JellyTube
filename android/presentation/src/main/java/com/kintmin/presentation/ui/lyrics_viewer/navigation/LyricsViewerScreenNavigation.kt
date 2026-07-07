package com.kintmin.presentation.ui.lyrics_viewer.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.lyrics_viewer.LyricsViewerScreen
import kotlinx.serialization.Serializable

@Serializable
data class LyricsViewerScreenRoute(val audioMediaId: Int)

fun NavController.navigateToLyricsViewerScreen(
    audioMediaId: Int,
    navOptions: NavOptions,
) = navigate(LyricsViewerScreenRoute(audioMediaId), navOptions)

fun NavGraphBuilder.lyricsViewerScreen(
    navigateToBack: () -> Unit,
    navigateToLyricsEdit: (audioMediaId: Int) -> Unit,
) {
    composable<LyricsViewerScreenRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LyricsViewerScreenRoute>()
        LyricsViewerScreen(
            navigateToBack = navigateToBack,
            navigateToLyricsEdit = { navigateToLyricsEdit(route.audioMediaId) },
        )
    }
}
