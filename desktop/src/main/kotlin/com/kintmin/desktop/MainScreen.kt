package com.kintmin.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kintmin.fileshare.FileShareConstants
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.io.File

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
        onFilesDrop = viewModel::onFilesDrop,
        onBulkArtistChange = viewModel::onBulkArtistChange,
        onApplyBulkArtist = viewModel::onApplyBulkArtist,
        onClearAll = viewModel::onClearAll,
        onRetry = viewModel::onRetry,
        onRetryAllFailed = viewModel::onRetryAllFailed,
        onRemoveItem = viewModel::onRemoveItem,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun MainScreenContent(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    onFilesDrop: (List<File>) -> Unit,
    onBulkArtistChange: (String) -> Unit,
    onApplyBulkArtist: () -> Unit,
    onClearAll: () -> Unit,
    onRetry: (String) -> Unit,
    onRetryAllFailed: () -> Unit,
    onRemoveItem: (String) -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
    val currentOnFilesDrop by rememberUpdatedState(onFilesDrop)
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isDragging = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDragging = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragging = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragging = false
                val transferable = event.awtTransferable
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                @Suppress("UNCHECKED_CAST")
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                    ?: return false
                currentOnFilesDrop(files)
                return true
            }
        }
    }
    val isConnected = uiState.discoveryState == DiscoveryState.FOUND
    val onPickAudioFiles = {
        onFilesDrop(pickFiles("업로드할 음원 선택", FileShareConstants.UPLOAD_AUDIO_EXTENSIONS, multiple = true))
    }
    val onPickThumbnail = {
        onFilesDrop(pickFiles("썸네일 이미지 선택", THUMBNAIL_IMAGE_EXTENSIONS, multiple = false))
    }

    // Modifier.background 로 칠하면 글자색(LocalContentColor)이 따라오지 않아 다크 테마에서 검게 남는다.
    Surface(color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dropTarget,
                )
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val header: @Composable () -> Unit = {
                Text(
                    text = "JellyTube 파일 공유",
                    style = MaterialTheme.typography.headlineSmall,
                )
                DiscoveryStatusCard(
                    discoveryState = uiState.discoveryState,
                    dropMessage = uiState.dropMessage,
                    canRefresh = !uiState.isUploading,
                    onRefresh = onRefresh,
                )
            }
            val bulkControls: @Composable () -> Unit = {
                BulkControls(
                    compact = maxWidth < EXPANDED_MIN_WIDTH,
                    uiState = uiState,
                    onBulkArtistChange = onBulkArtistChange,
                    onApplyBulkArtist = onApplyBulkArtist,
                    onPickThumbnail = onPickThumbnail,
                )
            }
            val uploadList: @Composable (Modifier) -> Unit = { modifier ->
                UploadList(
                    uiState = uiState,
                    onRetry = onRetry,
                    onRetryAllFailed = onRetryAllFailed,
                    onRemove = onRemoveItem,
                    onClearAll = onClearAll,
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
                        FileDropCard(
                            compact = false,
                            isConnected = isConnected,
                            isDragging = isDragging,
                            onPickAudioFiles = onPickAudioFiles,
                            modifier = Modifier.weight(1f),
                        )
                        bulkControls()
                    }
                    uploadList(Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    header()
                    FileDropCard(
                        compact = true,
                        isConnected = isConnected,
                        isDragging = isDragging,
                        onPickAudioFiles = onPickAudioFiles,
                    )
                    bulkControls()
                    uploadList(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DiscoveryStatusCard(
    discoveryState: DiscoveryState,
    dropMessage: String?,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
) {
    val contentColor = when (discoveryState) {
        DiscoveryState.FOUND -> MaterialTheme.colorScheme.onPrimaryContainer
        DiscoveryState.NOT_FOUND -> MaterialTheme.colorScheme.onErrorContainer
        DiscoveryState.DISCOVERING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (discoveryState) {
                DiscoveryState.FOUND -> MaterialTheme.colorScheme.primaryContainer
                DiscoveryState.NOT_FOUND -> MaterialTheme.colorScheme.errorContainer
                DiscoveryState.DISCOVERING -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = contentColor,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (discoveryState) {
                DiscoveryState.DISCOVERING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                DiscoveryState.FOUND -> Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                DiscoveryState.NOT_FOUND -> Icon(Icons.Rounded.Error, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (discoveryState) {
                        DiscoveryState.DISCOVERING -> "기기 검색 중..."
                        DiscoveryState.FOUND -> "기기 연결됨"
                        DiscoveryState.NOT_FOUND -> "기기를 찾을 수 없습니다"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (discoveryState == DiscoveryState.NOT_FOUND) {
                    Text(
                        text = "휴대폰 앱의 설정 → PC에서 공유받기 → 파일 공유 받기 시작을 누른 뒤 새로고침해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                dropMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (discoveryState != DiscoveryState.DISCOVERING) {
                IconAction(
                    icon = Icons.Rounded.Refresh,
                    label = if (canRefresh) "기기 다시 찾기" else "업로드가 끝나면 다시 찾을 수 있습니다",
                    enabled = canRefresh,
                    onClick = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun FileDropCard(
    compact: Boolean,
    isConnected: Boolean,
    isDragging: Boolean,
    onPickAudioFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (isDragging) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = if (isDragging) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        val title = when {
            isDragging -> "놓으면 바로 업로드합니다"
            isConnected -> "음원 파일을 끌어다 놓으세요"
            else -> "기기가 연결되면 업로드할 수 있습니다"
        }
        val caption = "여러 곡은 이름순으로 들어갑니다"
        val pickButton: @Composable () -> Unit = {
            OutlinedButton(onClick = onPickAudioFiles, enabled = isConnected) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("음원 파일 선택…")
            }
        }
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(caption, style = MaterialTheme.typography.bodySmall)
                }
                pickButton()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(48.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Text(caption, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Spacer(Modifier.size(4.dp))
                pickButton()
            }
        }
    }
}

@Composable
private fun BulkControls(
    compact: Boolean,
    uiState: MainUiState,
    onBulkArtistChange: (String) -> Unit,
    onApplyBulkArtist: () -> Unit,
    onPickThumbnail: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (uiState.successCount > 0) "업로드 완료 ${uiState.successCount}곡에 일괄 적용" else "업로드가 끝난 곡에 일괄 적용",
                style = MaterialTheme.typography.titleSmall,
            )
            val thumbnailButton: @Composable (Modifier) -> Unit = { modifier ->
                OutlinedButton(
                    onClick = onPickThumbnail,
                    enabled = uiState.canApplyThumbnail,
                    modifier = modifier,
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("썸네일 이미지 선택…")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.bulkArtist,
                    onValueChange = onBulkArtistChange,
                    label = { Text("아티스트") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onApplyBulkArtist, enabled = uiState.canApplyArtist) {
                    if (uiState.isApplyingBulk) {
                        CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    }
                    Text("적용")
                }
                if (compact) thumbnailButton(Modifier)
            }
            if (!compact) thumbnailButton(Modifier.fillMaxWidth())
            uiState.bulkMessage?.let {
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
    uiState: MainUiState,
    onRetry: (String) -> Unit,
    onRetryAllFailed: () -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("업로드 목록", style = MaterialTheme.typography.titleMedium)
                if (uiState.uploadItems.isNotEmpty()) {
                    Text(
                        text = summaryOf(uiState),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (uiState.failureCount > 0) {
                TextButton(onClick = onRetryAllFailed) { Text("실패 항목 다시 시도") }
            }
            TextButton(onClick = onClearAll, enabled = uiState.canClearList) { Text("목록 비우기") }
        }
        if (uiState.uploadItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "업로드한 파일이 여기 표시됩니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    // 스크롤바가 항목을 가리지 않게 그 폭만큼 비운다.
                    contentPadding = PaddingValues(end = 16.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.uploadItems, key = { it.id }) { item ->
                        UploadListItem(
                            item = item,
                            onRetry = { onRetry(item.id) },
                            onRemove = { onRemove(item.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                // 데스크톱 기본 스크롤바 색은 테마와 무관한 검정이라 다크 테마에서 보이지 않는다.
                val onSurface = MaterialTheme.colorScheme.onSurface
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    style = LocalScrollbarStyle.current.copy(
                        unhoverColor = onSurface.copy(alpha = 0.25f),
                        hoverColor = onSurface.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        }
    }
}

private fun summaryOf(uiState: MainUiState): String {
    val inProgress = uiState.uploadItems.size - uiState.successCount - uiState.failureCount
    return listOfNotNull(
        "${uiState.uploadItems.size}곡",
        "완료 ${uiState.successCount}",
        "진행 중 $inProgress".takeIf { inProgress > 0 },
        "실패 ${uiState.failureCount}".takeIf { uiState.failureCount > 0 },
    ).joinToString(" · ")
}

@Composable
private fun UploadListItem(
    item: UploadFileItem,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    maxLines = if (item.status == UploadStatus.FAILURE) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.status == UploadStatus.UPLOADING) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            Crossfade(targetState = item.status) { status -> StatusIcon(status) }
            if (item.status == UploadStatus.FAILURE) {
                IconAction(
                    icon = Icons.Rounded.RestartAlt,
                    label = "다시 시도",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onRetry,
                )
            }
            IconAction(
                icon = Icons.Rounded.Close,
                label = if (item.status == UploadStatus.QUEUED || item.status == UploadStatus.UPLOADING) "업로드 취소" else "목록에서 빼기",
                onClick = onRemove,
            )
        }
    }
}

@Composable
private fun StatusIcon(status: UploadStatus) {
    val modifier = Modifier.size(24.dp)
    when (status) {
        UploadStatus.QUEUED -> Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        UploadStatus.UPLOADING -> Icon(Icons.Rounded.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = modifier)
        UploadStatus.STAGED -> Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        UploadStatus.SAVING -> CircularProgressIndicator(modifier = modifier, strokeWidth = 2.dp)
        UploadStatus.SUCCESS -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = modifier)
        UploadStatus.FAILURE -> Icon(Icons.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = modifier)
    }
}

private fun statusLabel(item: UploadFileItem): String = when (item.status) {
    UploadStatus.QUEUED -> "업로드 대기 중"
    UploadStatus.UPLOADING -> "업로드 중 ${(item.progress * 100).toInt()}%"
    UploadStatus.STAGED -> "함께 올린 곡을 기다리는 중"
    UploadStatus.SAVING -> "휴대폰에 저장 중..."
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

/** 아이콘만 있는 버튼은 마우스를 올리면 하는 일을 글로 보여 준다. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    TooltipArea(
        tooltip = {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        },
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = label, tint = tint ?: LocalContentColor.current)
        }
    }
}

/** 창을 닫을 때까지 막는 모달이다. */
private fun pickFiles(title: String, extensions: Set<String>, multiple: Boolean): List<File> {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
        isMultipleMode = multiple
        // Windows 의 FileDialog 는 FilenameFilter 를 무시하고, 파일 이름 칸의 패턴만 걸러 준다.
        file = extensions.joinToString(";") { "*.$it" }
        setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in extensions }
        isVisible = true
    }
    return dialog.files.toList()
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
            errorMessage = "전송이 중간에 끊겼습니다. 다시 보내 주세요.",
        ),
    ),
    bulkArtist = "Sample Artist",
    bulkMessage = "아티스트 적용 완료",
)

@Composable
private fun MainScreenPreviewContent() {
    MainScreenContent(
        uiState = previewState,
        onRefresh = {},
        onFilesDrop = {},
        onBulkArtistChange = {},
        onApplyBulkArtist = {},
        onClearAll = {},
        onRetry = {},
        onRetryAllFailed = {},
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

/** MD3 window size class 의 Expanded 시작 폭. */
private val EXPANDED_MIN_WIDTH = 840.dp
private val SIDE_PANE_WIDTH = 360.dp

/** 넓은 모니터에서 목록 줄이 끝없이 늘어나지 않게 묶는 폭. */
private val MAX_CONTENT_WIDTH = 1040.dp
