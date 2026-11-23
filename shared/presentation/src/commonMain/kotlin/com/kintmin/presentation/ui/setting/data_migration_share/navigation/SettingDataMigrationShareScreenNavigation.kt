package com.kintmin.presentation.ui.setting.data_migration_share.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.data_migration_share.SettingDataMigrationShareScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingDataMigrationShareScreenRoute

fun NavController.navigateToSettingDataMigrationShareScreen(
    navOptions: NavOptions,
) = navigate(SettingDataMigrationShareScreenRoute, navOptions)

fun NavGraphBuilder.settingDataMigrationShareScreen(
    navigateToBack: () -> Unit,
    navigateToSync: () -> Unit,
    navigateToPlaylistSelect: () -> Unit,
    navigateToAudioMediaSelect: () -> Unit,
) {
    composable<SettingDataMigrationShareScreenRoute> {
        SettingDataMigrationShareScreen(
            navigateToBack = navigateToBack,
            navigateToSync = navigateToSync,
            navigateToPlaylistSelect = navigateToPlaylistSelect,
            navigateToAudioMediaSelect = navigateToAudioMediaSelect,
        )
    }
}
