package com.kintmin.presentation.ui.lyrics_detail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.lyrics_detail.LyricsDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class LyricsDetailScreenRoute(
    val audioMediaId: Int,
    val trackName: String,
    val artistName: String,
    val plainLyrics: String,
    val syncedLyrics: String,
)

fun NavController.navigateToLyricsDetailScreen(
    audioMediaId: Int,
    trackName: String,
    artistName: String,
    plainLyrics: String,
    syncedLyrics: String,
    navOptions: NavOptions,
) = navigate(
    LyricsDetailScreenRoute(audioMediaId, trackName, artistName, plainLyrics, syncedLyrics),
    navOptions,
)

fun NavGraphBuilder.lyricsDetailScreen(
    navigateToBack: () -> Unit,
) {
    composable<LyricsDetailScreenRoute> {
        LyricsDetailScreen(
            navigateToBack = navigateToBack,
        )
    }
}
