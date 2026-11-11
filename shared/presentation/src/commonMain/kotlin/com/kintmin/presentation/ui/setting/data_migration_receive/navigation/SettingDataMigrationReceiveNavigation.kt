package com.kintmin.presentation.ui.setting.data_migration_receive.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.data_migration_receive.SettingDataMigrationReceiveScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingDataMigrationReceiveScreenRoute

fun NavController.navigateToSettingDataMigrationReceiveScreen(
    navOptions: NavOptions,
) = navigate(SettingDataMigrationReceiveScreenRoute, navOptions)

fun NavGraphBuilder.settingDataMigrationReceiveScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingDataMigrationReceiveScreenRoute> {
        SettingDataMigrationReceiveScreen(
            navigateToBack = navigateToBack,
        )
    }
}
