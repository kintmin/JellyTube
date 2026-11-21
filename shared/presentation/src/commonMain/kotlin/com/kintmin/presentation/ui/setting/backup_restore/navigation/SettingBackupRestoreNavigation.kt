package com.kintmin.presentation.ui.setting.backup_restore.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.kintmin.presentation.ui.setting.backup_restore.SettingBackupRestoreScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingBackupRestoreScreenRoute

fun NavController.navigateToSettingBackupRestoreScreen(
    navOptions: NavOptions,
) = navigate(SettingBackupRestoreScreenRoute, navOptions)

fun NavGraphBuilder.settingBackupRestoreScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingBackupRestoreScreenRoute> {
        SettingBackupRestoreScreen(
            navigateToBack = navigateToBack,
        )
    }
}
