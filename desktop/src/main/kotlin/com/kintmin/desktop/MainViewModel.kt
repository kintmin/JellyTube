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

data class MainUiState(
    val discoveryState: DiscoveryState = DiscoveryState.DISCOVERING,
    val pendingFile: PendingFileItem? = null,
)

enum class DiscoveryState { DISCOVERING, FOUND, NOT_FOUND }

data class PendingFileItem(
    val file: File,
    val status: UploadStatus = UploadStatus.IDLE,
    val errorMessage: String? = null,
)

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

    fun onFileDrop(file: File) {
        _uiState.update { it.copy(pendingFile = PendingFileItem(file = file)) }
    }

    fun onUpload() {
        val file = _uiState.value.pendingFile?.file ?: return
        val currentUploader = uploader ?: return
        if (_uiState.value.pendingFile?.status == UploadStatus.UPLOADING) return

        _uiState.update { it.copy(pendingFile = it.pendingFile?.copy(status = UploadStatus.UPLOADING)) }
        scope.launch {
            val result = currentUploader.uploadFile(file)
            result.onSuccess { response ->
                if (response.success) {
                    _uiState.update {
                        it.copy(pendingFile = it.pendingFile?.copy(status = UploadStatus.SUCCESS, errorMessage = null))
                    }
                } else {
                    _uiState.update {
                        it.copy(pendingFile = it.pendingFile?.copy(status = UploadStatus.FAILURE, errorMessage = response.message))
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(pendingFile = it.pendingFile?.copy(status = UploadStatus.FAILURE, errorMessage = e.message ?: "업로드 실패"))
                }
            }
        }
    }

    fun onRetry() {
        onUpload()
    }

    fun onClearFile() {
        _uiState.update { it.copy(pendingFile = null) }
    }

    fun onDispose() {
        uploader?.close()
        scope.cancel()
    }
}
