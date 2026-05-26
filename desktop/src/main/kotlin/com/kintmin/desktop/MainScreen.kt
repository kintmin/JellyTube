package com.kintmin.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.unit.dp
import com.kintmin.fileshare.UploadStatus
import java.awt.datatransfer.DataFlavor
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen() {
    val viewModel = remember { MainViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.onDispose() }
    }

    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                @Suppress("UNCHECKED_CAST")
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                val audioFile = files?.firstOrNull { isAudioFile(it) } ?: return false
                viewModel.onFileDrop(audioFile)
                return true
            }
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dropTarget,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 타이틀
                Text(
                    text = "JellyTube 파일 공유",
                    style = MaterialTheme.typography.headlineMedium,
                )

                // 기기 검색 상태 카드
                DiscoveryStatusCard(
                    discoveryState = uiState.discoveryState,
                    onRefresh = viewModel::startDiscovery,
                )

                // 파일 업로드 영역
                if (uiState.discoveryState == DiscoveryState.FOUND) {
                    FileDropCard(
                        pendingFile = uiState.pendingFile,
                        onUpload = viewModel::onUpload,
                        onRetry = viewModel::onRetry,
                        onClear = viewModel::onClearFile,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveryStatusCard(
    discoveryState: DiscoveryState,
    onRefresh: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (discoveryState) {
                DiscoveryState.FOUND -> MaterialTheme.colorScheme.primaryContainer
                DiscoveryState.NOT_FOUND -> MaterialTheme.colorScheme.errorContainer
                DiscoveryState.DISCOVERING -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (discoveryState) {
                DiscoveryState.DISCOVERING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                DiscoveryState.FOUND -> Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                DiscoveryState.NOT_FOUND -> Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (discoveryState) {
                        DiscoveryState.DISCOVERING -> "Android 기기 검색 중..."
                        DiscoveryState.FOUND -> "기기 연결됨"
                        DiscoveryState.NOT_FOUND -> "기기를 찾을 수 없습니다"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = when (discoveryState) {
                        DiscoveryState.FOUND -> MaterialTheme.colorScheme.onPrimaryContainer
                        DiscoveryState.NOT_FOUND -> MaterialTheme.colorScheme.onErrorContainer
                        DiscoveryState.DISCOVERING -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (discoveryState == DiscoveryState.NOT_FOUND) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "앱 설정 → 파일 공유 받기 → 파일 공유 받기 시작 버튼을 눌러주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            if (discoveryState != DiscoveryState.DISCOVERING) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "새로고침",
                        tint = when (discoveryState) {
                            DiscoveryState.FOUND -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileDropCard(
    pendingFile: PendingFileItem?,
    onUpload: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (pendingFile == null) {
                // 드래그 앤 드롭 안내
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Upload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "음원 파일을 이 곳에 끌어다 놓으세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "mp3, wav, flac 등 지원",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column {
                            Text(
                                text = pendingFile.file.name,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = statusLabel(pendingFile.status),
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor(pendingFile.status),
                            )
                        }
                    }

                    pendingFile.errorMessage?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (pendingFile.status) {
                            UploadStatus.IDLE -> Button(onClick = onUpload) {
                                Text("업로드")
                            }
                            UploadStatus.UPLOADING -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            UploadStatus.SUCCESS -> {
                                Text(
                                    text = "업로드 성공",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                OutlinedButton(onClick = onClear) { Text("닫기") }
                            }
                            UploadStatus.FAILURE -> {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text("재시도")
                                }
                                OutlinedButton(onClick = onClear) { Text("취소") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: UploadStatus): String = when (status) {
    UploadStatus.IDLE -> "업로드 대기 중"
    UploadStatus.UPLOADING -> "업로드 중..."
    UploadStatus.SUCCESS -> "업로드 완료"
    UploadStatus.FAILURE -> "업로드 실패"
}

@Composable
private fun statusColor(status: UploadStatus) = when (status) {
    UploadStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    UploadStatus.UPLOADING -> MaterialTheme.colorScheme.secondary
    UploadStatus.SUCCESS -> MaterialTheme.colorScheme.primary
    UploadStatus.FAILURE -> MaterialTheme.colorScheme.error
}

private fun isAudioFile(file: File): Boolean {
    val audioExtensions = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "aiff")
    return file.extension.lowercase() in audioExtensions
}
