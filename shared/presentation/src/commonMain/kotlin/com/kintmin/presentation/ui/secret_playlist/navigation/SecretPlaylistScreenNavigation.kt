package com.kintmin.presentation.ui.secret_playlist.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.secret_playlist.SecretPlaylistScreen
import kotlinx.serialization.Serializable

@Serializable
object SecretPlaylistScreenRoute

fun NavController.navigateToSecretPlaylistScreen(
    navOptions: NavOptions,
) = navigate(SecretPlaylistScreenRoute, navOptions)

fun NavGraphBuilder.secretPlaylistScreen(
    navigateToBack: () -> Unit,
    navigateToPlaylistDetail: (playlistId: Int, secretOnly: Boolean) -> Unit,
    navigateToPlaylistEdit: (playlistId: Int) -> Unit,
    navigateToPlaylistAdd: (playlistId: Int, playlistName: String) -> Unit,
    navigateToPlayerDetail: () -> Unit,
) {
    composable<SecretPlaylistScreenRoute> {
        SecretPlaylistScreen(
            navigateToBack = navigateToBack,
            navigateToPlaylistDetail = navigateToPlaylistDetail,
            navigateToPlaylistEdit = navigateToPlaylistEdit,
            navigateToPlaylistAdd = navigateToPlaylistAdd,
            navigateToPlayerDetail = navigateToPlayerDetail,
        )
    }
}
