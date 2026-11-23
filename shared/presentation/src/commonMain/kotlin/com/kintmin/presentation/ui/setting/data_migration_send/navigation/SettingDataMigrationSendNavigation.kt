package com.kintmin.presentation.ui.setting.data_migration_send.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import com.kintmin.presentation.navigation.EnumNavType
import com.kintmin.presentation.ui.setting.data_migration_send.SettingDataMigrationSendScreen
import com.kintmin.presentation.ui.setting.data_migration_share.MigrationShareKind
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable

/** [idList] 는 [kind] 에 따라 플레이리스트 id 이거나 곡 id 다. 동기화에서는 비어 있다. */
@Serializable
data class SettingDataMigrationSendScreenRoute(
    val kind: MigrationShareKind,
    val idList: List<Int> = emptyList(),
)

internal val settingDataMigrationSendTypeMap: Map<KType, NavType<*>> =
    mapOf(typeOf<MigrationShareKind>() to EnumNavType(MigrationShareKind.entries))

fun NavController.navigateToSettingDataMigrationSendScreen(
    kind: MigrationShareKind,
    navOptions: NavOptions,
    idList: List<Int> = emptyList(),
) = navigate(SettingDataMigrationSendScreenRoute(kind, idList), navOptions)

fun NavGraphBuilder.settingDataMigrationSendScreen(
    navigateToBack: () -> Unit,
) {
    composable<SettingDataMigrationSendScreenRoute>(typeMap = settingDataMigrationSendTypeMap) {
        SettingDataMigrationSendScreen(
            navigateToBack = navigateToBack,
        )
    }
}
