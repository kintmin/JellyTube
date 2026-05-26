package com.kintmin.desktop

import com.kintmin.desktop.discovery.DiscoveredDevice
import com.kintmin.desktop.discovery.discoverDevice
import com.kintmin.desktop.upload.FileUploader
import com.kintmin.fileshare.UploadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var discoveredDevice: DiscoveredDevice? = null
    private var uploader: FileUploader? = null

    init {
        startDiscovery()
    }

    fun startDiscovery() {
        _uiState.update { it.copy(discoveryState = DiscoveryState.DISCOVERING) }
        scope.launch {
            val device = discoverDevice(timeoutMs = 3_000L)
            discoveredDevice = device
            uploader?.close()
            uploader = device?.let { FileUploader(it.hostAddress, it.port) }
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
        val newItems = files.map { UploadFileItem(file = it) }
        _uiState.update {
            it.copy(
                uploadItems = it.uploadItems + newItems,
                bulkMessage = null,
            )
        }
        newItems.forEach { item -> uploadItem(item.id, item.file, currentUploader) }
    }

    fun onRetry(itemId: String) {
        val item = _uiState.value.uploadItems.firstOrNull { it.id == itemId } ?: return
        val currentUploader = uploader ?: return
        if (item.status == UploadStatus.UPLOADING) return
        _uiState.updateItem(itemId) { it.copy(status = UploadStatus.UPLOADING, errorMessage = null) }
        uploadItem(itemId, item.file, currentUploader)
    }

    fun onRemoveItem(itemId: String) {
        _uiState.update { state ->
            state.copy(uploadItems = state.uploadItems.filterNot { it.id == itemId })
        }
    }

    fun onClearAll() {
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

    private fun uploadItem(itemId: String, file: File, currentUploader: FileUploader) {
        scope.launch {
            val result = currentUploader.uploadFile(file)
            result.onSuccess { response ->
                if (response.success) {
                    _uiState.updateItem(itemId) {
                        it.copy(
                            status = UploadStatus.SUCCESS,
                            errorMessage = null,
                            audioMediaId = response.audioMediaId,
                            title = response.title,
                        )
                    }
                } else {
                    _uiState.updateItem(itemId) {
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

    private fun successAudioMediaIds(): List<Int> = _uiState.value.uploadItems
        .filter { it.status == UploadStatus.SUCCESS }
        .mapNotNull { it.audioMediaId }

    fun onDispose() {
        uploader?.close()
        scope.cancel()
    }
}

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
