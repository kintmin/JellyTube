package com.kintmin.presentation.ui.main.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.main.MainScreen
import com.kintmin.presentation.ui.main.MainScreenIntent
import com.kintmin.presentation.ui.main.MainTabItem
import com.kintmin.presentation.ui.main.MainViewModel
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
    navigateToPlaylistDetail: (playlistId: Int) -> Unit,
    navigateToPlaylistEdit: (playlistId: Int) -> Unit,
    navigateToPlaylistAdd: (playlistId: Int) -> Unit,
) {
    composable<MainScreenRoute> { backStackEntry ->
        MainScreen(
            navigateToPlaylistDetail = navigateToPlaylistDetail,
            navigateToPlaylistEdit = navigateToPlaylistEdit,
            navigateToPlaylistAdd = navigateToPlaylistAdd,
        )
    }
}
