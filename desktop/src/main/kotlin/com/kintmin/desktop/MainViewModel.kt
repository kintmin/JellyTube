package com.kintmin.desktop

import com.kintmin.desktop.discovery.DiscoveredDevice
import com.kintmin.desktop.discovery.discoverDevice
import com.kintmin.desktop.upload.FileUploader
import com.kintmin.desktop.upload.naturalFileNameOrder
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

    /** 묶음은 드롭한 차례대로 반영한다. 뒤 묶음이 먼저 다 받아져도 앞 묶음의 반영을 기다린다. */
    private var lastCommit: Job? = null

    init {
        startDiscovery()
    }

    fun startDiscovery() {
        _uiState.update { it.copy(discoveryState = DiscoveryState.DISCOVERING) }
        scope.launch {
            val device = discover()
            uploader?.close()
            uploader = device?.let(createUploader)
            _uiState.update {
                it.copy(discoveryState = if (device != null) DiscoveryState.FOUND else DiscoveryState.NOT_FOUND)
            }
        }
    }

    fun onAudioFilesDrop(files: List<File>) {
        val currentUploader = uploader ?: run {
            _uiState.update { it.copy(bulkMessage = "연결된 기기가 없습니다.") }
            return
        }
        val newItems = files.sortedWith(naturalFileNameOrder).map { UploadFileItem(file = it) }
        _uiState.update {
            it.copy(
                uploadItems = it.uploadItems + newItems,
                bulkMessage = null,
            )
        }
        uploadBatch(newItems.map { it.id }, currentUploader)
    }

    fun onRetry(itemId: String) {
        val item = _uiState.value.uploadItems.firstOrNull { it.id == itemId } ?: return
        val currentUploader = uploader ?: return
        if (item.status != UploadStatus.FAILURE) return
        _uiState.updateItem(itemId) {
            it.copy(status = UploadStatus.QUEUED, progress = 0f, stageId = null, errorMessage = null)
        }
        uploadBatch(listOf(itemId), currentUploader)
    }

    fun onRemoveItem(itemId: String) {
        stageJobs.remove(itemId)?.cancel()
        _uiState.update { state ->
            state.copy(uploadItems = state.uploadItems.filterNot { it.id == itemId })
        }
    }

    fun onClearAll() {
        stageJobs.values.forEach { it.cancel() }
        stageJobs.clear()
        _uiState.update { it.copy(uploadItems = emptyList(), bulkMessage = null) }
    }

    fun onBulkArtistChange(value: String) {
        _uiState.update { it.copy(bulkArtist = value) }
    }

    fun onApplyBulkArtist() {
        val artist = _uiState.value.bulkArtist.trim()
        if (artist.isEmpty()) {
            _uiState.update { it.copy(bulkMessage = "아티스트를 입력해주세요.") }
            return
        }
        val ids = successAudioMediaIds()
        if (ids.isEmpty()) {
            _uiState.update { it.copy(bulkMessage = "업로드 완료된 음원이 없습니다.") }
            return
        }
        val currentUploader = uploader ?: return
        _uiState.update { it.copy(bulkMessage = "아티스트 적용 중...") }
        scope.launch {
            currentUploader.updateArtist(ids, artist)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(bulkMessage = if (response.success) "아티스트 적용 완료" else response.message)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(bulkMessage = error.message ?: "아티스트 적용 실패") }
                }
        }
    }

    fun onImageDrop(imageFile: File) {
        val ids = successAudioMediaIds()
        if (ids.isEmpty()) {
            _uiState.update { it.copy(bulkMessage = "업로드 완료된 음원이 없습니다.") }
            return
        }
        val currentUploader = uploader ?: return
        _uiState.update { it.copy(bulkMessage = "썸네일 적용 중...") }
        scope.launch {
            currentUploader.updateThumbnail(ids, imageFile)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(bulkMessage = if (response.success) "썸네일 적용 완료" else response.message)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(bulkMessage = error.message ?: "썸네일 적용 실패") }
                }
        }
    }

    fun onUnsupportedImageDrop() {
        _uiState.update { it.copy(bulkMessage = "이미지는 1장만 적용할 수 있습니다.") }
    }

    /** 곡은 병렬로 보내고, 묶음 전체가 받아진 뒤 [itemIds] 순서 그대로 한 번에 반영한다. */
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
