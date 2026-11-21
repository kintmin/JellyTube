package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import kotlinx.coroutines.flow.first

/** 비밀모드를 끄고, 다운로드 대상이 비밀 플레이리스트면 미분류로 되돌린다. */
class EndSecretModeUseCase constructor(
    private val secretModeRepository: SecretModeRepository,
    private val appSettingRepository: AppSettingRepository,
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        secretModeRepository.setEnabled(false)
        val savedId = appSettingRepository.getPlaylistIdOnDownloadFlow().first()
        val playlists = playlistRepository.getAllPlaylistFlow().first()
        if (playlists.none { it.id == savedId && it.type == PlaylistType.SECRET }) return Result.success(Unit)
        val uncategorizedId = playlists.firstOrNull { it.type == PlaylistType.UNCATEGORIZED }?.id
            ?: return Result.success(Unit)
        return appSettingRepository.updatePlaylistIdOnDownload(uncategorizedId)
    }
}
