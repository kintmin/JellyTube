package com.kintmin.presentation.ui.lyrics_search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.lyrics_search.LyricsSearchScreen
import kotlinx.serialization.Serializable

@Serializable
data class LyricsSearchScreenRoute(
    val audioMediaId: Int,
    val initialQuery: String,
    val durationSeconds: Double? = null,
)

fun NavController.navigateToLyricsSearchScreen(
    audioMediaId: Int,
    initialQuery: String,
    durationSeconds: Double?,
    navOptions: NavOptions,
) = navigate(LyricsSearchScreenRoute(audioMediaId, initialQuery, durationSeconds), navOptions)

fun NavGraphBuilder.lyricsSearchScreen(
    navigateToBack: () -> Unit,
    navigateToLyricsDetail: (
        audioMediaId: Int,
        trackName: String,
        artistName: String,
        plainLyrics: String,
        syncedLyrics: String,
    ) -> Unit,
    navigateToLyricsEdit: (audioMediaId: Int) -> Unit,
) {
    composable<LyricsSearchScreenRoute> {
        LyricsSearchScreen(
            navigateToBack = navigateToBack,
            navigateToLyricsDetail = navigateToLyricsDetail,
            navigateToLyricsEdit = navigateToLyricsEdit,
        )
    }
}
