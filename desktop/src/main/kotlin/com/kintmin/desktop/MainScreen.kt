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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kintmin.fileshare.UploadStatus
import org.jetbrains.compose.ui.tooling.preview.Preview
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

    MainScreenContent(
        uiState = uiState,
        onRefresh = viewModel::startDiscovery,
        onAudioFilesDrop = viewModel::onAudioFilesDrop,
        onImageDrop = viewModel::onImageDrop,
        onUnsupportedImageDrop = viewModel::onUnsupportedImageDrop,
        onBulkArtistChange = viewModel::onBulkArtistChange,
        onApplyBulkArtist = viewModel::onApplyBulkArtist,
        onClearAll = viewModel::onClearAll,
        onRetry = viewModel::onRetry,
        onRemoveItem = viewModel::onRemoveItem,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun MainScreenContent(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    onAudioFilesDrop: (List<File>) -> Unit,
    onImageDrop: (File) -> Unit,
    onUnsupportedImageDrop: () -> Unit,
    onBulkArtistChange: (String) -> Unit,
    onApplyBulkArtist: () -> Unit,
    onClearAll: () -> Unit,
    onRetry: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
) {
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                @Suppress("UNCHECKED_CAST")
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                    ?: return false
                val audioFiles = files.filter { isAudioFile(it) }
                val imageFiles = files.filter { isImageFile(it) }
                when {
                    audioFiles.isNotEmpty() -> onAudioFilesDrop(audioFiles)
                    imageFiles.size == 1 -> onImageDrop(imageFiles.first())
                    imageFiles.size > 1 -> onUnsupportedImageDrop()
                    else -> return false
                }
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
                Text(
                    text = "JellyTube 파일 공유",
                    style = MaterialTheme.typography.headlineMedium,
                )

                DiscoveryStatusCard(
                    discoveryState = uiState.discoveryState,
                    onRefresh = onRefresh,
                )

                if (uiState.discoveryState == DiscoveryState.FOUND) {
                    FileDropCard()
                    BulkControls(
                        bulkArtist = uiState.bulkArtist,
                        bulkMessage = uiState.bulkMessage,
                        hasItems = uiState.uploadItems.isNotEmpty(),
                        onBulkArtistChange = onBulkArtistChange,
                        onApplyBulkArtist = onApplyBulkArtist,
                        onClearAll = onClearAll,
                    )
                    UploadList(
                        uploadItems = uiState.uploadItems,
                        onRetry = onRetry,
                        onRemove = onRemoveItem,
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
private fun FileDropCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
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
                    text = "음원 파일을 끌어다 놓으면 즉시 업로드됩니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "mp3, wav, flac 등 여러 음원 지원 · 이미지 1장은 성공 항목 썸네일로 적용",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BulkControls(
    bulkArtist: String,
    bulkMessage: String?,
    hasItems: Boolean,
    onBulkArtistChange: (String) -> Unit,
    onApplyBulkArtist: () -> Unit,
    onClearAll: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = bulkArtist,
                    onValueChange = onBulkArtistChange,
                    label = { Text("아티스트 일괄 적용") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onApplyBulkArtist) {
                    Text("적용")
                }
                OutlinedButton(
                    onClick = onClearAll,
                    enabled = hasItems,
                ) {
                    Text("전체 삭제")
                }
            }
            bulkMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UploadList(
    uploadItems: List<UploadFileItem>,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(uploadItems, key = { it.id }) { item ->
            UploadListItem(
                item = item,
                onRetry = { onRetry(item.id) },
                onRemove = { onRemove(item.id) },
            )
        }
    }
}

@Composable
private fun UploadListItem(
    item: UploadFileItem,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title ?: item.file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.errorMessage ?: statusLabel(item.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(item.status),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusIcon(status = item.status)
            if (item.status == UploadStatus.FAILURE) {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "재시도",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "삭제",
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(status: UploadStatus) {
    when (status) {
        UploadStatus.IDLE -> Icon(
            imageVector = Icons.Rounded.AudioFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        UploadStatus.UPLOADING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        UploadStatus.SUCCESS -> Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "업로드 완료",
            tint = MaterialTheme.colorScheme.primary,
        )
        UploadStatus.FAILURE -> Icon(
            imageVector = Icons.Rounded.Error,
            contentDescription = "업로드 실패",
            tint = MaterialTheme.colorScheme.error,
        )
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

@Preview
@Composable
private fun MainScreenPreview() {
    MainScreenContent(
        uiState = MainUiState(
            discoveryState = DiscoveryState.FOUND,
            uploadItems = listOf(
                UploadFileItem(
                    file = File("sample-track.mp3"),
                    status = UploadStatus.SUCCESS,
                    audioMediaId = 1,
                    title = "Sample Track",
                ),
                UploadFileItem(
                    file = File("uploading-track.flac"),
                    status = UploadStatus.UPLOADING,
                ),
                UploadFileItem(
                    file = File("failed-track.wav"),
                    status = UploadStatus.FAILURE,
                    errorMessage = "업로드 실패",
                ),
            ),
            bulkArtist = "Sample Artist",
            bulkMessage = "업로드 완료 항목에 일괄 적용할 수 있습니다.",
        ),
        onRefresh = {},
        onAudioFilesDrop = {},
        onImageDrop = {},
        onUnsupportedImageDrop = {},
        onBulkArtistChange = {},
        onApplyBulkArtist = {},
        onClearAll = {},
        onRetry = {},
        onRemoveItem = {},
    )
}

private fun isAudioFile(file: File): Boolean {
    val audioExtensions = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "aiff")
    return file.extension.lowercase() in audioExtensions
}

private fun isImageFile(file: File): Boolean {
    val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    return file.extension.lowercase() in imageExtensions
}
