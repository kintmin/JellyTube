package com.kintmin.presentation.ui.setting.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kintmin.presentation.ui.setting.SettingScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingGraph

@Serializable
object SettingScreenRoute

fun NavController.navigateToSettingScreen(
    navOptions: NavOptions,
) = navigate(SettingScreenRoute, navOptions)

fun NavGraphBuilder.settingGraph(
    navigateToBack: () -> Unit,
    navigateToStep: () -> Unit,
    navigateToAppLog: () -> Unit,
    settingDestination: NavGraphBuilder.() -> Unit,
) {
    navigation<SettingGraph>(startDestination = SettingScreenRoute) {
        composable<SettingScreenRoute> {
            SettingScreen(
                navigateToBack = navigateToBack,
                navigateToStep = navigateToStep,
                navigateToAppLog = navigateToAppLog,
            )
        }

        settingDestination()
    }
}
