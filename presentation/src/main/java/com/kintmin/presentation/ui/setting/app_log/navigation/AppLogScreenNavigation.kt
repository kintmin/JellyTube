package com.kintmin.presentation.ui.setting.app_log.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.app_log.AppLogScreen
import kotlinx.serialization.Serializable

@Serializable
object AppLogScreenRoute

fun NavController.navigateToAppLogScreen(
    navOptions: NavOptions,
) = navigate(AppLogScreenRoute, navOptions)

fun NavGraphBuilder.appLogScreen(
    navigateToBack: () -> Unit,
) {
    composable<AppLogScreenRoute> {
        AppLogScreen(
            navigateToBack = navigateToBack,
        )
    }
}
