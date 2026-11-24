package com.kintmin.presentation.ui.setting.data_migration_share_audio.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.data_migration_share_audio.SettingDataMigrationShareAudioScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingDataMigrationShareAudioScreenRoute

fun NavController.navigateToSettingDataMigrationShareAudioScreen(
    navOptions: NavOptions,
) = navigate(SettingDataMigrationShareAudioScreenRoute, navOptions)

fun NavGraphBuilder.settingDataMigrationShareAudioScreen(
    navigateToBack: () -> Unit,
    navigateToSend: (audioMediaIdList: List<Int>) -> Unit,
) {
    composable<SettingDataMigrationShareAudioScreenRoute> {
        SettingDataMigrationShareAudioScreen(
            navigateToBack = navigateToBack,
            navigateToSend = navigateToSend,
        )
    }
}
