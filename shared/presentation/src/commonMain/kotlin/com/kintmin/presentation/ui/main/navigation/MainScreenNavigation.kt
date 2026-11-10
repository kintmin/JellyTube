package com.kintmin.presentation.ui.main.navigation

import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.main.MainScreen
import com.kintmin.presentation.navigation.EnumNavType
import com.kintmin.presentation.ui.main.MainTabItem
import kotlinx.serialization.Serializable
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
data class MainScreenRoute(
    val tabItem: MainTabItem,
    val searchUrl: String? = null,
)

/** 등록부와 `toRoute()` 가 같은 것을 써야 한다. 이유는 [EnumNavType]. */
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
    navigateToPlaylistAdd: (playlistId: Int) -> Unit,
    navigateToSetting: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    navigateToFileShareReceive: () -> Unit,
) {
    composable<MainScreenRoute>(typeMap = mainScreenTypeMap) { backStackEntry ->
        MainScreen(
            isDebugTabVisible = isDebugTabVisible,
            navigateToPlaylistDetail = navigateToPlaylistDetail,
            navigateToPlaylistEdit = navigateToPlaylistEdit,
            navigateToPlaylistAdd = navigateToPlaylistAdd,
            navigateToSetting = navigateToSetting,
            navigateToPlayerDetail = navigateToPlayerDetail,
            navigateToFileShareReceive = navigateToFileShareReceive,
        )
    }
}
