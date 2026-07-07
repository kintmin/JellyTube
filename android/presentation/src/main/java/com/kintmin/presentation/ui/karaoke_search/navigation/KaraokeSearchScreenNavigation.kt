package com.kintmin.presentation.ui.karaoke_search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.karaoke_search.KaraokeSearchScreen
import kotlinx.serialization.Serializable

@Serializable
data class KaraokeSearchScreenRoute(
    val audioMediaId: Int,
    val initialQuery: String,
)

fun NavController.navigateToKaraokeSearchScreen(
    audioMediaId: Int,
    initialQuery: String,
    navOptions: NavOptions,
) = navigate(KaraokeSearchScreenRoute(audioMediaId, initialQuery), navOptions)

fun NavGraphBuilder.karaokeSearchScreen(
    navigateToBack: () -> Unit,
) {
    composable<KaraokeSearchScreenRoute> {
        KaraokeSearchScreen(
            navigateToBack = navigateToBack,
        )
    }
}
