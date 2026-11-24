package com.kintmin.presentation.ui.setting.data_migration_share_playlist.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.data_migration_share_playlist.SettingDataMigrationSharePlaylistScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingDataMigrationSharePlaylistScreenRoute

fun NavController.navigateToSettingDataMigrationSharePlaylistScreen(
    navOptions: NavOptions,
) = navigate(SettingDataMigrationSharePlaylistScreenRoute, navOptions)

fun NavGraphBuilder.settingDataMigrationSharePlaylistScreen(
    navigateToBack: () -> Unit,
    navigateToSend: (playlistIdList: List<Int>) -> Unit,
) {
    composable<SettingDataMigrationSharePlaylistScreenRoute> {
        SettingDataMigrationSharePlaylistScreen(
            navigateToBack = navigateToBack,
            navigateToSend = navigateToSend,
        )
    }
}
