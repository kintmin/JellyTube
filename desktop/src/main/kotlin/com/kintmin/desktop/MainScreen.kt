package com.kintmin.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kintmin.fileshare.FileShareConstants
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
internal fun MainScreenContent(
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dropTarget,
                )
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isConnected = uiState.discoveryState == DiscoveryState.FOUND
            val header: @Composable () -> Unit = {
                Text(
                    text = "JellyTube 파일 공유",
                    style = MaterialTheme.typography.headlineMedium,
                )
                DiscoveryStatusCard(
                    discoveryState = uiState.discoveryState,
                    onRefresh = onRefresh,
                )
            }
            val bulkControls: @Composable () -> Unit = {
                BulkControls(
                    bulkArtist = uiState.bulkArtist,
                    bulkMessage = uiState.bulkMessage,
                    hasItems = uiState.uploadItems.isNotEmpty(),
                    onBulkArtistChange = onBulkArtistChange,
                    onApplyBulkArtist = onApplyBulkArtist,
                    onClearAll = onClearAll,
                )
            }
            val uploadList: @Composable (Modifier) -> Unit = { modifier ->
                UploadList(
                    uploadItems = uiState.uploadItems,
                    onRetry = onRetry,
                    onRemove = onRemoveItem,
                    modifier = modifier,
                )
            }
            val contentModifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxSize()

            if (maxWidth >= EXPANDED_MIN_WIDTH) {
                Row(
                    modifier = contentModifier,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        modifier = Modifier.width(SIDE_PANE_WIDTH).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        header()
                        if (isConnected) {
                            FileDropCard(compact = false, modifier = Modifier.weight(1f))
                            bulkControls()
                        }
                    }
                    if (isConnected) uploadList(Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    header()
                    if (isConnected) {
                        FileDropCard(compact = true)
                        bulkControls()
                        uploadList(Modifier.weight(1f))
                    }
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
                        DiscoveryState.DISCOVERING -> "기기 검색 중..."
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
                        text = "앱 설정 → PC에서 공유받기 → 파일 공유 받기 시작 버튼을 눌러주세요.",
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
private fun FileDropCard(compact: Boolean, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        val title = "음원 파일을 끌어다 놓으면 바로 보냅니다"
        val caption = "mp3, wav, flac 등 여러 음원 지원 · 이미지 1장은 성공 항목 썸네일로 적용"
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
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
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            // 스크롤바가 항목을 가리지 않게 그 폭만큼 비운다.
            contentPadding = PaddingValues(end = 16.dp),
            modifier = Modifier.fillMaxSize(),
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
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
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
                    text = item.errorMessage ?: statusLabel(item),
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
        UploadStatus.QUEUED, UploadStatus.STAGED -> Icon(
            imageVector = Icons.Rounded.AudioFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        UploadStatus.UPLOADING, UploadStatus.SAVING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
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
private fun statusLabel(item: UploadFileItem): String = when (item.status) {
    UploadStatus.QUEUED -> "업로드 대기 중"
    UploadStatus.UPLOADING -> "업로드 중 ${(item.progress * 100).toInt()}%"
    UploadStatus.STAGED -> "함께 보낸 곡을 기다리는 중"
    UploadStatus.SAVING -> "앱에 저장 중..."
    UploadStatus.SUCCESS -> "업로드 완료"
    UploadStatus.FAILURE -> "업로드 실패"
}

@Composable
private fun statusColor(status: UploadStatus) = when (status) {
    UploadStatus.QUEUED, UploadStatus.STAGED -> MaterialTheme.colorScheme.onSurfaceVariant
    UploadStatus.UPLOADING, UploadStatus.SAVING -> MaterialTheme.colorScheme.secondary
    UploadStatus.SUCCESS -> MaterialTheme.colorScheme.primary
    UploadStatus.FAILURE -> MaterialTheme.colorScheme.error
}

private val previewState = MainUiState(
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
            progress = 0.42f,
        ),
        UploadFileItem(
            file = File("failed-track.wav"),
            status = UploadStatus.FAILURE,
            errorMessage = "업로드 실패",
        ),
    ),
    bulkArtist = "Sample Artist",
    bulkMessage = "업로드 완료 항목에 일괄 적용할 수 있습니다.",
)

@Composable
private fun MainScreenPreviewContent() {
    MainScreenContent(
        uiState = previewState,
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

@Preview
@Composable
private fun MainScreenCompactPreview() {
    Box(Modifier.size(width = 560.dp, height = 720.dp)) { MainScreenPreviewContent() }
}

@Preview
@Composable
private fun MainScreenExpandedPreview() {
    Box(Modifier.size(width = 1040.dp, height = 720.dp)) { MainScreenPreviewContent() }
}

private fun isAudioFile(file: File): Boolean = file.extension.lowercase() in FileShareConstants.UPLOAD_AUDIO_EXTENSIONS

private fun isImageFile(file: File): Boolean {
    val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    return file.extension.lowercase() in imageExtensions
}

/** MD3 window size class 의 Expanded 시작 폭. */
private val EXPANDED_MIN_WIDTH = 840.dp
private val SIDE_PANE_WIDTH = 360.dp

/** 넓은 모니터에서 목록 줄이 끝없이 늘어나지 않게 묶는 폭. */
private val MAX_CONTENT_WIDTH = 1040.dp
