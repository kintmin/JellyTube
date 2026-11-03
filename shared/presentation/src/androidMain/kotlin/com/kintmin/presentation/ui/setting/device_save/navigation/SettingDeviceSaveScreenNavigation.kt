package com.kintmin.presentation.ui.setting.device_save.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.device_save.SettingDeviceSaveScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingDeviceSaveScreenRoute

fun NavController.navigateToSettingDeviceSaveScreen(
    navOptions: NavOptions,
) = navigate(SettingDeviceSaveScreenRoute, navOptions)

fun NavGraphBuilder.settingDeviceSaveScreen(
    navigateToBack: () -> Unit,
    navigateToDeviceSaveSelect: () -> Unit,
) {
    composable<SettingDeviceSaveScreenRoute> {
        SettingDeviceSaveScreen(
            navigateToBack = navigateToBack,
            navigateToDeviceSaveSelect = navigateToDeviceSaveSelect,
        )
    }
}
