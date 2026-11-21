package com.kintmin.presentation.ui.main.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kintmin.presentation.navigation.EnumNavType
import com.kintmin.presentation.ui.main.MainScreen
import com.kintmin.presentation.ui.main.MainTabItem
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable

@Serializable
data class MainScreenRoute(
    val tabItem: MainTabItem,
    val searchUrl: String? = null,
    val hintSecretPlaylistEntry: Boolean = false,
)

internal val mainScreenTypeMap: Map<KType, NavType<*>> =
    mapOf(typeOf<MainTabItem>() to EnumNavType(MainTabItem.entries))

fun NavController.navigateToMainScreen(
    tabItem: MainTabItem,
    navOptions: NavOptions,
    searchUrl: String? = null,
) = navigate(MainScreenRoute(tabItem, searchUrl), navOptions)

fun NavGraphBuilder.mainScreen(
    isDebugTabVisible: Boolean,
    navigateToPlaylistDetail: (playlistId: Int, isBasePlaylist: Boolean) -> Unit,
    navigateToPlaylistEdit: (playlistId: Int) -> Unit,
    navigateToPlaylistAdd: (playlistId: Int, playlistName: String) -> Unit,
    navigateToSetting: () -> Unit,
    navigateToSecretPlaylist: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    navigateToPlaylistDownload: (url: String) -> Unit,
) {
    composable<MainScreenRoute>(typeMap = mainScreenTypeMap) { backStackEntry ->
        MainScreen(
            isDebugTabVisible = isDebugTabVisible,
            hintSecretPlaylistEntry = backStackEntry.toRoute<MainScreenRoute>().hintSecretPlaylistEntry,
            navigateToPlaylistDetail = navigateToPlaylistDetail,
            navigateToPlaylistEdit = navigateToPlaylistEdit,
            navigateToPlaylistAdd = navigateToPlaylistAdd,
            navigateToSetting = navigateToSetting,
            navigateToSecretPlaylist = navigateToSecretPlaylist,
            navigateToPlayerDetail = navigateToPlayerDetail,
            navigateToPlaylistDownload = navigateToPlaylistDownload,
        )
    }
}
