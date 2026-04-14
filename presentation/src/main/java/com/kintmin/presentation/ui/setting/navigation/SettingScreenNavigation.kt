package com.kintmin.presentation.ui.setting.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.SettingScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingScreenRoute

fun NavController.navigateToSettingScreen(
    navOptions: NavOptions,
) = navigate(SettingScreenRoute, navOptions)

fun NavGraphBuilder.settingScreen(
    navigateToBack: () -> Unit,
    navigateToAppLog: () -> Unit,
) {
    composable<SettingScreenRoute> {
        SettingScreen(
            navigateToBack = navigateToBack,
            navigateToAppLog = navigateToAppLog,
        )
    }
}
