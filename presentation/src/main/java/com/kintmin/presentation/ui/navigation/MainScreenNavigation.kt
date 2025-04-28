package com.kintmin.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.MainScreen
import com.kintmin.presentation.ui.MainTabItem
import kotlinx.serialization.Serializable

@Serializable
data class MainScreenRoute(val tabItem: MainTabItem)

fun NavController.navigateToMainScreen(
    tabItem: MainTabItem,
    navOptions: NavOptions,
) = navigate(MainScreenRoute(tabItem), navOptions)

fun NavGraphBuilder.mainScreen(navigateToPlaylistDetail: (id: Int) -> Unit) {
    composable<MainScreenRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MainScreenRoute>()
        MainScreen(
            initTabItem = route.tabItem,
            navigateToPlaylistDetail = navigateToPlaylistDetail,
        )
    }
}
