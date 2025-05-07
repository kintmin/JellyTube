package com.kintmin.presentation.ui.main.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.main.MainScreen
import com.kintmin.presentation.ui.main.MainTabItem
import kotlinx.serialization.Serializable

@Serializable
data class MainScreenRoute(val tabItem: MainTabItem)

fun NavController.navigateToMainScreen(
    tabItem: MainTabItem,
    navOptions: NavOptions,
) = navigate(MainScreenRoute(tabItem), navOptions)

fun NavGraphBuilder.mainScreen(
    navigateToPlaylistDetail: (playlistId: Int) -> Unit,
    navigateToPlaylistEdit: (playlistId: Int) -> Unit,
    navigateToPlaylistAdd: (playlistId: Int) -> Unit,
) {
    composable<MainScreenRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MainScreenRoute>()
        MainScreen(
            initTabItem = route.tabItem,
            navigateToPlaylistDetail = navigateToPlaylistDetail,
            navigateToPlaylistEdit = navigateToPlaylistEdit,
            navigateToPlaylistAdd = navigateToPlaylistAdd,
        )
    }
}
