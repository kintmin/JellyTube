package com.kintmin.presentation.ui.setting.file_share_receive.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.file_share_receive.SettingFileShareReceiveScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingFileShareReceiveScreenRoute

fun NavController.navigateToSettingFileShareReceiveScreen(
    navOptions: NavOptions,
) = navigate(SettingFileShareReceiveScreenRoute, navOptions)

fun NavGraphBuilder.settingFileShareReceiveScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingFileShareReceiveScreenRoute> {
        SettingFileShareReceiveScreen(
            navigateToBack = navigateToBack,
        )
    }
}
