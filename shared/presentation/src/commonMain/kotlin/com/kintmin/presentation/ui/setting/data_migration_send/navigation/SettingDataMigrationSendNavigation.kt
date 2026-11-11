package com.kintmin.presentation.ui.setting.data_migration_send.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.data_migration_send.SettingDataMigrationSendScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingDataMigrationSendScreenRoute

fun NavController.navigateToSettingDataMigrationSendScreen(
    navOptions: NavOptions,
) = navigate(SettingDataMigrationSendScreenRoute, navOptions)

fun NavGraphBuilder.settingDataMigrationSendScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingDataMigrationSendScreenRoute> {
        SettingDataMigrationSendScreen(
            navigateToBack = navigateToBack,
        )
    }
}
