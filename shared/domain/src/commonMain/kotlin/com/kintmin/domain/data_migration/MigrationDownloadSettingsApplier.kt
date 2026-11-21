package com.kintmin.domain.data_migration

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.playlist.model.PlaylistType

/** 다운로드 설정은 받는 기기 것을 덮는다. 대상 플레이리스트를 못 찾으면 미분류로 둔다(설정이 없을 때와 같다). */
internal suspend fun AppSettingRepository.applyMigratedDownloadSettings(
    settings: MigrationDownloadSettings,
    plan: MigrationPlan,
    applied: MigrationApplyResult,
    target: MigrationSnapshot,
) {
    updateShouldInsertAtTopOnDownload(settings.shouldInsertAtTop).getOrThrow()
    val playlistId = when (val ref = settings.playlistId?.let { plan.playlistRefBySourceId[it] }) {
        is MigrationRef.Existing -> ref.id
        is MigrationRef.Incoming -> applied.newPlaylistIdBySourceId[ref.sourceId]
        null -> null
    } ?: target.playlists.first { it.type == PlaylistType.UNCATEGORIZED }.id
    updatePlaylistIdOnDownload(playlistId).getOrThrow()
}
