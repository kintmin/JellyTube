package com.kintmin.data.file_share

import android.content.Context
import com.kintmin.data.file_share.migration.MigrationSessionHolder
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.data_migration.MigrationSendHandler
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository
import com.kintmin.domain.file_share.PcUploadHandler
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import com.kintmin.log.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/**
 * PC 수신과 데이터 보내기가 한 서버(한 포트)를 같이 쓴다. 둘 중 하나라도 켜져 있으면 서버가 떠 있다.
 * 프로세스를 살려 두는 포그라운드 서비스는 android:app 이 따로 띄운다.
 */
internal class AndroidFileShareServerRepository(
    context: Context,
    appLog: AppLog,
) : PcFileShareServerRepository, MigrationSendServerRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val owners = MutableStateFlow<Set<FileShareServerOwner>>(emptySet())

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    override val status: StateFlow<DataMigrationSendStatus> = MigrationSessionHolder.state

    private var pcUploadHandler: PcUploadHandler? = null
    private var migrationSendHandler: MigrationSendHandler? = null

    private val server = FileShareServer(scope, appLog) {
        fileShareModule(
            pcUploadHandler = CurrentPcUploadHandler(),
            uploadCacheDir = context.cacheDir,
            currentMigrationSession = { MigrationSessionHolder.session.value },
            migrationSendHandler = CurrentMigrationSendHandler(),
        )
    }
    private val nsdAdvertiser = NsdAdvertiser(context, scope)

    @Synchronized
    override fun start(handler: PcUploadHandler) {
        pcUploadHandler = handler
        updateOwners(owners.value + FileShareServerOwner.PC_FILE_SHARE)
    }

    @Synchronized
    override fun stop() {
        updateOwners(owners.value - FileShareServerOwner.PC_FILE_SHARE)
        pcUploadHandler = null
    }

    @Synchronized
    override fun open(handler: MigrationSendHandler) {
        migrationSendHandler = handler
        MigrationSessionHolder.open()
        updateOwners(owners.value + FileShareServerOwner.DATA_MIGRATION)
    }

    @Synchronized
    override fun close() {
        MigrationSessionHolder.close()
        updateOwners(owners.value - FileShareServerOwner.DATA_MIGRATION)
        migrationSendHandler = null
    }

    private fun updateOwners(next: Set<FileShareServerOwner>) {
        val previous = owners.value
        owners.value = next
        _isRunning.value = FileShareServerOwner.PC_FILE_SHARE in next
        when {
            previous.isEmpty() && next.isNotEmpty() -> {
                server.launch()
                nsdAdvertiser.start(combine(owners, MigrationSessionHolder.state, ::advertisedServiceTypes))
            }
            previous.isNotEmpty() && next.isEmpty() -> {
                nsdAdvertiser.stop()
                server.stop()
            }
        }
    }

    // 한쪽만 켜져 있어도 서버에는 두 경로가 다 있다. 꺼진 쪽으로 온 요청은 처리하지 않는다.
    private inner class CurrentPcUploadHandler : PcUploadHandler {
        override fun validateFileName(rawFileName: String?) =
            pcUploadHandler?.validateFileName(rawFileName) ?: stopped(PC_FILE_SHARE_STOPPED)

        override suspend fun importUploadedAudio(uploadedFileFullPath: String, originalFileName: String): Result<ImportedAudioMediaResult> =
            pcUploadHandler?.importUploadedAudio(uploadedFileFullPath, originalFileName) ?: stopped(PC_FILE_SHARE_STOPPED)

        override suspend fun applyArtist(audioMediaIds: List<Int>, artist: String) =
            pcUploadHandler?.applyArtist(audioMediaIds, artist) ?: stopped(PC_FILE_SHARE_STOPPED)

        override suspend fun applyThumbnail(audioMediaIds: List<Int>, imageData: ByteArray) =
            pcUploadHandler?.applyThumbnail(audioMediaIds, imageData) ?: stopped(PC_FILE_SHARE_STOPPED)
    }

    private inner class CurrentMigrationSendHandler : MigrationSendHandler {
        override suspend fun cleanup() = migrationSendHandler?.cleanup() ?: stopped(MIGRATION_SEND_STOPPED)

        override suspend fun export(): Result<MigrationExport> = migrationSendHandler?.export() ?: stopped(MIGRATION_SEND_STOPPED)

        override fun resolveFilePath(file: MigrationFile) = migrationSendHandler?.resolveFilePath(file) ?: stopped(MIGRATION_SEND_STOPPED)
    }

    private fun <T> stopped(message: String): Result<T> = Result.failure(IllegalStateException(message))
}

private const val PC_FILE_SHARE_STOPPED = "PC 수신이 꺼져 있습니다."
private const val MIGRATION_SEND_STOPPED = "데이터 보내기가 꺼져 있습니다."
