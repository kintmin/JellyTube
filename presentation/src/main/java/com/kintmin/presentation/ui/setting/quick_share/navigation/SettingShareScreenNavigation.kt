package com.kintmin.presentation.ui.setting.quick_share.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.quick_share.SettingShareScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingShareScreenRoute

fun NavController.navigateToSettingShareScreen(
    navOptions: NavOptions,
) = navigate(SettingShareScreenRoute, navOptions)

fun NavGraphBuilder.settingShareScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingShareScreenRoute> {
        SettingShareScreen(
            navigateToBack = navigateToBack,
        )
    }
}
