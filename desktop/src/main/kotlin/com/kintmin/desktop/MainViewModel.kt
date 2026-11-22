package com.kintmin.desktop

import com.kintmin.desktop.discovery.DiscoveredDevice
import com.kintmin.desktop.discovery.discoverDevice
import com.kintmin.desktop.upload.FileUploader
import com.kintmin.desktop.upload.naturalFileNameOrder
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.FileShareResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MainViewModel(
    private val discover: suspend () -> DiscoveredDevice? = { discoverDevice(timeoutMs = 3_000L) },
    private val createUploader: (DiscoveredDevice) -> FileUploader = { FileUploader(it.hostAddress, it.port) },
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var uploader: FileUploader? = null

    // 받는 기기의 Wi-Fi 대역이 한계라 더 늘려도 빨라지지 않고, 요청마다 받는 쪽에 임시 파일이 하나씩 생긴다.
    private val stageSlots = Semaphore(MAX_PARALLEL_STAGES)
    private val stageJobs = ConcurrentHashMap<String, Job>()

    private var lastCommit: Job? = null

    init {
        startDiscovery()
    }

    fun startDiscovery() {
        if (_uiState.value.isUploading) return
        _uiState.update { it.copy(discoveryState = DiscoveryState.DISCOVERING, dropMessage = null) }
        scope.launch {
            val device = discover()
            uploader?.close()
            uploader = device?.let(createUploader)
            _uiState.update {
                it.copy(discoveryState = if (device != null) DiscoveryState.FOUND else DiscoveryState.NOT_FOUND)
            }
        }
    }

    fun onFilesDrop(files: List<File>) {
        val audioFiles = files.filter { it.extension.lowercase() in FileShareConstants.UPLOAD_AUDIO_EXTENSIONS }
        val imageFiles = files.filter { it.extension.lowercase() in THUMBNAIL_IMAGE_EXTENSIONS }
        when {
            audioFiles.isNotEmpty() -> {
                val skipped = files.size - audioFiles.size
                sendAudioFiles(audioFiles, skippedMessage = if (skipped > 0) "음원이 아닌 파일 ${skipped}개는 건너뛰었습니다." else null)
            }
            imageFiles.size == 1 -> applyThumbnail(imageFiles.single())
            imageFiles.size > 1 -> _uiState.update { it.copy(dropMessage = "썸네일 이미지는 1장만 놓을 수 있습니다.") }
            else -> _uiState.update { it.copy(dropMessage = UNSUPPORTED_FILES_MESSAGE) }
        }
    }

    fun onRetry(itemId: String) {
        val item = itemOf(itemId) ?: return
        if (item.status != UploadStatus.FAILURE) return
        retry(listOf(itemId))
    }

    fun onRetryAllFailed() {
        retry(_uiState.value.uploadItems.filter { it.status == UploadStatus.FAILURE }.map { it.id })
    }

    fun onRemoveItem(itemId: String) {
        stageJobs.remove(itemId)?.cancel()
        _uiState.update { state ->
            state.copy(uploadItems = state.uploadItems.filterNot { it.id == itemId })
        }
    }

    fun onClearAll() {
        if (!_uiState.value.canClearList) return
        _uiState.update { it.copy(uploadItems = emptyList(), bulkMessage = null, dropMessage = null) }
    }

    fun onBulkArtistChange(value: String) {
        _uiState.update { it.copy(bulkArtist = value) }
    }

    fun onApplyBulkArtist() {
        val state = _uiState.value
        if (!state.canApplyArtist) return
        val artist = state.bulkArtist.trim()
        applyBulk(progressMessage = "아티스트 적용 중...", doneMessage = "아티스트 적용 완료", failMessage = "아티스트 적용 실패") { uploader, ids ->
            uploader.updateArtist(ids, artist)
        }
    }

    private fun applyThumbnail(imageFile: File) {
        if (_uiState.value.isApplyingBulk) return
        if (_uiState.value.successCount == 0) {
            _uiState.update { it.copy(bulkMessage = "업로드 완료된 음원이 없어 썸네일을 적용할 곳이 없습니다.") }
            return
        }
        applyBulk(progressMessage = "썸네일 적용 중...", doneMessage = "썸네일 적용 완료", failMessage = "썸네일 적용 실패") { uploader, ids ->
            uploader.updateThumbnail(ids, imageFile)
        }
    }

    private fun applyBulk(
        progressMessage: String,
        doneMessage: String,
        failMessage: String,
        request: suspend (FileUploader, List<Int>) -> Result<FileShareResponse>,
    ) {
        val currentUploader = uploader ?: return
        val ids = successAudioMediaIds()
        _uiState.update { it.copy(isApplyingBulk = true, bulkMessage = progressMessage) }
        scope.launch {
            val message = request(currentUploader, ids).fold(
                onSuccess = { response -> if (response.success) doneMessage else response.message },
                onFailure = { error -> error.message ?: failMessage },
            )
            _uiState.update { it.copy(isApplyingBulk = false, bulkMessage = message) }
        }
    }

    private fun sendAudioFiles(files: List<File>, skippedMessage: String?) {
        val currentUploader = uploader ?: run {
            _uiState.update { it.copy(dropMessage = NOT_CONNECTED_MESSAGE) }
            return
        }
        val newItems = files.sortedWith(naturalFileNameOrder).map { UploadFileItem(file = it) }
        _uiState.update {
            it.copy(uploadItems = it.uploadItems + newItems, dropMessage = skippedMessage)
        }
        uploadBatch(newItems.map { it.id }, currentUploader)
    }

    private fun retry(itemIds: List<String>) {
        if (itemIds.isEmpty()) return
        val currentUploader = uploader ?: run {
            _uiState.update { it.copy(dropMessage = NOT_CONNECTED_MESSAGE) }
            return
        }
        itemIds.forEach { id ->
            _uiState.updateItem(id) {
                it.copy(status = UploadStatus.QUEUED, progress = 0f, stageId = null, errorMessage = null)
            }
        }
        uploadBatch(itemIds, currentUploader)
    }

    private fun uploadBatch(itemIds: List<String>, currentUploader: FileUploader) {
        val stages = itemIds.associateWith { itemId -> stage(itemId, currentUploader) }
        stageJobs.putAll(stages)
        val previousCommit = lastCommit
        lastCommit = scope.launch {
            stages.values.joinAll()
            stageJobs.keys.removeAll(stages.keys)
            previousCommit?.join()
            commit(itemIds, currentUploader)
        }
    }

    private fun stage(itemId: String, currentUploader: FileUploader): Job = scope.launch {
        val file = itemOf(itemId)?.file ?: return@launch
        stageSlots.withPermit {
            _uiState.updateItem(itemId) { it.copy(status = UploadStatus.UPLOADING, progress = 0f) }
            var lastPercent = 0
            currentUploader.stage(file) { progress ->
                // 전송 조각마다 불리므로 1% 단위로만 상태를 바꾼다.
                val percent = (progress * 100).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    _uiState.updateItem(itemId) { it.copy(progress = progress) }
                }
            }.onSuccess { response ->
                _uiState.updateItem(itemId) {
                    if (response.success && response.stageId != null) {
                        it.copy(status = UploadStatus.STAGED, progress = 1f, stageId = response.stageId)
                    } else {
                        it.copy(status = UploadStatus.FAILURE, errorMessage = response.message)
                    }
                }
            }.onFailure { e ->
                _uiState.updateItem(itemId) {
                    it.copy(status = UploadStatus.FAILURE, errorMessage = e.message ?: "업로드 실패")
                }
            }
        }
    }

    private suspend fun commit(itemIds: List<String>, currentUploader: FileUploader) {
        val staged = itemIds.mapNotNull(::itemOf).filter { it.status == UploadStatus.STAGED }
        if (staged.isEmpty()) return
        staged.forEach { item -> _uiState.updateItem(item.id) { it.copy(status = UploadStatus.SAVING) } }
        currentUploader.commit(staged.map { it.stageId!! })
            .onSuccess { response ->
                staged.zip(response.results).forEach { (item, result) ->
                    _uiState.updateItem(item.id) {
                        if (result.success) {
                            it.copy(
                                status = UploadStatus.SUCCESS,
                                errorMessage = null,
                                audioMediaId = result.audioMediaId,
                                title = result.title,
                            )
                        } else {
                            it.copy(status = UploadStatus.FAILURE, errorMessage = result.message)
                        }
                    }
                }
            }
            .onFailure { e ->
                staged.forEach { item ->
                    _uiState.updateItem(item.id) {
                        it.copy(status = UploadStatus.FAILURE, errorMessage = e.message ?: "앱에 저장하지 못했습니다.")
                    }
                }
            }
    }

    private fun itemOf(itemId: String): UploadFileItem? = _uiState.value.uploadItems.firstOrNull { it.id == itemId }

    private fun successAudioMediaIds(): List<Int> = _uiState.value.uploadItems
        .filter { it.status == UploadStatus.SUCCESS }
        .mapNotNull { it.audioMediaId }

    fun onDispose() {
        uploader?.close()
        scope.cancel()
    }
}

private const val MAX_PARALLEL_STAGES = 3

internal val THUMBNAIL_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

private const val NOT_CONNECTED_MESSAGE = "기기가 연결되지 않아 업로드할 수 없습니다. 휴대폰에서 받기를 켠 뒤 새로고침해 주세요."
private const val UNSUPPORTED_FILES_MESSAGE = "음원(mp3·flac 등)이나 썸네일 이미지(jpg·png·webp)만 놓을 수 있습니다."

private fun MutableStateFlow<MainUiState>.updateItem(
    itemId: String,
    transform: (UploadFileItem) -> UploadFileItem,
) {
    update { state ->
        state.copy(
            uploadItems = state.uploadItems.map { item ->
                if (item.id == itemId) transform(item) else item
            },
        )
    }
}
