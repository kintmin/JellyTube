package com.kintmin.jellytube

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import com.kintmin.presentation.ui.MainTabItem
import com.kintmin.presentation.ui.playlist_detail.navigation.navigateToPlaylistDetailScreen
import com.kintmin.presentation.ui.playlist_detail.navigation.playlistDetail
import com.kintmin.presentation.ui.navigation.MainScreenRoute
import com.kintmin.presentation.ui.navigation.mainScreen

@Composable
fun MainNavHost(
    navController: NavHostController,
) {
    val navOptions = NavOptions.Builder().build()

    NavHost(
        navController = navController,
        startDestination = MainScreenRoute(MainTabItem.Playlist),
    ) {
        mainScreen(
            navigateToPlaylistDetail = { id ->
                navController.navigateToPlaylistDetailScreen(id, navOptions)
            }
        )
        playlistDetail(
            navigateToBack = {
                navController.popBackStack()
            }
        )
    }
}

//navOptions {
//    // Pop up to the start destination of the graph to
//    // avoid building up a large stack of destinations
//    // on the back stack as users select items
//    popUpTo(navController.graph.findStartDestination().id) {
//        saveState = true
//    }
//    // Avoid multiple copies of the same destination when
//    // reselecting the same item
//    launchSingleTop = true
//    // Restore state when reselecting a previously selected item
//    restoreState = true
//}