package com.kintmin.data.file_share

import com.kintmin.data.file_share.migration.MigrationSessionHolder
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.model.UploadedAudioFile
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.data_migration.MigrationSendHandler
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository
import com.kintmin.domain.file_share.PcUploadHandler
import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.log.AppLog
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * PC 수신과 데이터 보내기가 한 서버(한 포트)를 같이 쓴다. 둘 중 하나라도 켜져 있으면 서버가 떠 있다.
 */
internal class FileShareServerRepository(
    private val scope: CoroutineScope,
    appLog: AppLog,
    uploadTempDir: String,
    receiveKeepAlive: ReceiveKeepAlive,
    private val advertiser: ServiceAdvertiser,
) : PcFileShareServerRepository, MigrationSendServerRepository {

    private val lock = SynchronizedObject()

    private val owners = MutableStateFlow<Set<FileShareServerOwner>>(emptySet())

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    override val status: StateFlow<DataMigrationSendStatus> = MigrationSessionHolder.state

    private var pcUploadHandler: PcUploadHandler? = null
    private var migrationSendHandler: MigrationSendHandler? = null

    private val server = FileShareServer(scope, appLog) {
        fileShareModule(
            pcUploadHandler = CurrentPcUploadHandler(),
            appLog = appLog,
            uploadTempDir = uploadTempDir,
            receiveKeepAlive = receiveKeepAlive,
            currentMigrationSession = { MigrationSessionHolder.session.value },
            migrationSendHandler = CurrentMigrationSendHandler(),
        )
    }

    override fun start(handler: PcUploadHandler) {
        synchronized(lock) {
            pcUploadHandler = handler
            updateOwners(owners.value + FileShareServerOwner.PC_FILE_SHARE)
        }
    }

    override fun stop() {
        synchronized(lock) {
            updateOwners(owners.value - FileShareServerOwner.PC_FILE_SHARE)
            pcUploadHandler = null
        }
    }

    override fun open(handler: MigrationSendHandler) {
        synchronized(lock) {
            migrationSendHandler = handler
            MigrationSessionHolder.open()
            updateOwners(owners.value + FileShareServerOwner.DATA_MIGRATION)
        }
    }

    override fun close() {
        synchronized(lock) {
            MigrationSessionHolder.close()
            updateOwners(owners.value - FileShareServerOwner.DATA_MIGRATION)
            migrationSendHandler = null
        }
    }

    /**
     * 서스펜드된 동안 OS 가 리슨 소켓을 회수하거나 광고를 내릴 수 있다. 둘 다 오류 없이 조용히 사라지므로
     * 복귀할 때 서버가 살아 있는지 보고 광고를 다시 등록한다.
     */
    fun resumeAfterSuspension() {
        scope.launch {
            if (owners.value.isEmpty()) return@launch
            if (!isListening()) server.launch(onStartFailed = { _isRunning.value = false })
            advertiser.refresh()
        }
    }

    private suspend fun isListening(): Boolean = suspendRunCatching {
        SelectorManager(Dispatchers.IO).use { selector ->
            withTimeout(PROBE_TIMEOUT_MS) {
                aSocket(selector).tcp().connect(LOOPBACK_HOST, FileShareConstants.DEFAULT_PORT).close()
            }
        }
    }.isSuccess

    private fun updateOwners(next: Set<FileShareServerOwner>) {
        val previous = owners.value
        owners.value = next
        _isRunning.value = FileShareServerOwner.PC_FILE_SHARE in next
        when {
            previous.isEmpty() && next.isNotEmpty() -> {
                server.launch(onStartFailed = { _isRunning.value = false })
                advertiser.start(combine(owners, MigrationSessionHolder.state, ::advertisedServiceTypes))
            }
            previous.isNotEmpty() && next.isEmpty() -> {
                advertiser.stop()
                server.stop()
            }
        }
    }

    // 한쪽만 켜져 있어도 서버에는 두 경로가 다 있다. 꺼진 쪽으로 온 요청은 처리하지 않는다.
    private inner class CurrentPcUploadHandler : PcUploadHandler {
        override fun validateFileName(rawFileName: String?) =
            pcUploadHandler?.validateFileName(rawFileName) ?: stopped(PC_FILE_SHARE_STOPPED)

        override suspend fun importUploadedAudios(uploads: List<UploadedAudioFile>): List<Result<ImportedAudioMediaResult>> =
            pcUploadHandler?.importUploadedAudios(uploads) ?: uploads.map { stopped(PC_FILE_SHARE_STOPPED) }

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

private const val PROBE_TIMEOUT_MS = 1_000L
private const val LOOPBACK_HOST = "127.0.0.1"
private const val PC_FILE_SHARE_STOPPED = "PC 수신이 꺼져 있습니다."
private const val MIGRATION_SEND_STOPPED = "데이터 보내기가 꺼져 있습니다."
