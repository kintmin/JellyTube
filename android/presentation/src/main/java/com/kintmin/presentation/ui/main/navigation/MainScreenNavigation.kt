package com.kintmin.presentation.ui.main.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.main.MainScreen
import com.kintmin.presentation.ui.main.MainTabItem
import kotlinx.serialization.Serializable

@Serializable
data class MainScreenRoute(
    val tabItem: MainTabItem,
    val searchUrl: String? = null,
)

fun NavController.navigateToMainScreen(
    tabItem: MainTabItem,
    navOptions: NavOptions,
    searchUrl: String? = null,
) = navigate(MainScreenRoute(tabItem, searchUrl), navOptions)

fun NavGraphBuilder.mainScreen(
    navigateToPlaylistDetail: (playlistId: Int, isBasePlaylist: Boolean) -> Unit,
    navigateToPlaylistEdit: (playlistId: Int) -> Unit,
    navigateToPlaylistAdd: (playlistId: Int) -> Unit,
    navigateToSetting: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    navigateToFileShareReceive: () -> Unit,
) {
    composable<MainScreenRoute> { backStackEntry ->
        MainScreen(
            navigateToPlaylistDetail = navigateToPlaylistDetail,
            navigateToPlaylistEdit = navigateToPlaylistEdit,
            navigateToPlaylistAdd = navigateToPlaylistAdd,
            navigateToSetting = navigateToSetting,
            navigateToPlayerDetail = navigateToPlayerDetail,
            navigateToFileShareReceive = navigateToFileShareReceive,
        )
    }
}
