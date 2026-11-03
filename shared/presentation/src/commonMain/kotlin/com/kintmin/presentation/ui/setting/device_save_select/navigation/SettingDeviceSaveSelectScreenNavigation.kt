package com.kintmin.presentation.ui.setting.device_save_select.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.device_save_select.SettingDeviceSaveSelectScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingDeviceSaveSelectScreenRoute

fun NavController.navigateToSettingDeviceSaveSelectScreen(
    navOptions: NavOptions,
) = navigate(SettingDeviceSaveSelectScreenRoute, navOptions)

fun NavGraphBuilder.settingDeviceSaveSelectScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingDeviceSaveSelectScreenRoute> {
        SettingDeviceSaveSelectScreen(
            navigateToBack = navigateToBack,
        )
    }
}
