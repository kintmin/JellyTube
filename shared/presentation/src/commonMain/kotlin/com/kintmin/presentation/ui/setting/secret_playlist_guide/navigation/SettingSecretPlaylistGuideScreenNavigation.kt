package com.kintmin.presentation.ui.setting.secret_playlist_guide.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.secret_playlist_guide.SettingSecretPlaylistGuideScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingSecretPlaylistGuideScreenRoute

fun NavController.navigateToSettingSecretPlaylistGuideScreen(
    navOptions: NavOptions,
) = navigate(SettingSecretPlaylistGuideScreenRoute, navOptions)

fun NavGraphBuilder.settingSecretPlaylistGuideScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingSecretPlaylistGuideScreenRoute> {
        SettingSecretPlaylistGuideScreen(navigateToBack = navigateToBack)
    }
}
