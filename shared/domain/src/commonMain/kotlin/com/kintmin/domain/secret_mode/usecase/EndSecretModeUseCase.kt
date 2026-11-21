package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.flow.first

/** 비밀모드를 끄고, 다운로드 대상이 비밀 플레이리스트면 미분류로 되돌린다. */
class EndSecretModeUseCase constructor(
    private val secretModeRepository: SecretModeRepository,
    private val appSettingRepository: AppSettingRepository,
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(): Result<Unit> {
        if (secretModeRepository.isEnabledFlow.value) log("비밀모드 끔")
        secretModeRepository.setEnabled(false)
        val savedId = appSettingRepository.getPlaylistIdOnDownloadFlow().first()
        val playlists = playlistRepository.getAllPlaylistFlow().first()
        if (playlists.none { it.id == savedId && it.type == PlaylistType.SECRET }) return Result.success(Unit)
        val uncategorizedId = playlists.firstOrNull { it.type == PlaylistType.UNCATEGORIZED }?.id
            ?: return Result.success(Unit).also { log("미분류 플레이리스트가 없어 다운로드 대상이 비밀 플레이리스트로 남음") }
        return appSettingRepository.updatePlaylistIdOnDownload(uncategorizedId)
            .onSuccess { log("다운로드 대상을 비밀 플레이리스트에서 미분류로 되돌림") }
            .onFailure { appLog.sendErrorLog(ErrorLog(LOG_TAG, "다운로드 대상을 미분류로 되돌리지 못함", it)) }
    }

    private fun log(message: String) = appLog.sendDebugLog(DebugLog(LOG_TAG, message))
}

private const val LOG_TAG = "SecretMode"
