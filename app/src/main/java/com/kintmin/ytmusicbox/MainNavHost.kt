package com.kintmin.ytmusicbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kintmin.presentation.ui.MainTabItem
import com.kintmin.presentation.ui.navigation.MainScreenRoute
import com.kintmin.presentation.ui.navigation.mainScreen

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
) {
    val navController: NavHostController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = MainScreenRoute(MainTabItem.Play),
        modifier = modifier,
    ) {
        mainScreen()
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