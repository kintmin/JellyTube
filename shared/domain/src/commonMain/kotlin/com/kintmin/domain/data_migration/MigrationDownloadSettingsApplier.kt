package com.kintmin.domain.data_migration

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.playlist.model.PlaylistType

/**
 * 다운로드 설정은 받는 기기 것을 덮는다. 대상 플레이리스트를 못 찾거나 비밀 플레이리스트면 미분류로 둔다.
 * 비밀모드가 꺼진 채 비밀 플레이리스트가 대상이면 받은 곡이 어디에도 보이지 않는다.
 */
internal suspend fun AppSettingRepository.applyMigratedDownloadSettings(
    settings: MigrationDownloadSettings,
    plan: MigrationPlan,
    applied: MigrationApplyResult,
    target: MigrationSnapshot,
) {
    updateShouldInsertAtTopOnDownload(settings.shouldInsertAtTop).getOrThrow()
    val playlistId = when (val ref = settings.playlistId?.let { plan.playlistRefBySourceId[it] }) {
        is MigrationRef.Existing -> ref.id.takeIf { id -> target.playlists.none { it.id == id && it.type == PlaylistType.SECRET } }
        is MigrationRef.Incoming -> applied.newPlaylistIdBySourceId[ref.sourceId]
            .takeIf { plan.newPlaylists.none { it.id == ref.sourceId && it.type == PlaylistType.SECRET } }
        null -> null
    } ?: target.playlists.first { it.type == PlaylistType.UNCATEGORIZED }.id
    updatePlaylistIdOnDownload(playlistId).getOrThrow()
}
