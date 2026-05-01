package com.kintmin.presentation.ui.step.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.step.StepScreen
import kotlinx.serialization.Serializable

@Serializable
object StepScreenRoute

fun NavController.navigateToStepScreen(
    navOptions: NavOptions,
) = navigate(StepScreenRoute, navOptions)

fun NavGraphBuilder.stepScreen() {
    composable<StepScreenRoute> {
        StepScreen()
    }
}
