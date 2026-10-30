package com.kintmin.presentation.ui.audio_edit

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss_dot_sss
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_edit.dialog.AudioEditExitDialog
import com.kintmin.presentation.ui.audio_edit.dialog.AudioEditSaveDialog
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineRulerView
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineWaveformView
import com.kintmin.presentation.ui.audio_edit.waveform.rememberTimelineViewportState
import kotlin.time.Duration.Companion.milliseconds
import org.koin.androidx.compose.koinViewModel

@Composable
fun AudioEditScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<AudioEditViewModel>()
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AudioEditEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                AudioEditEvent.NavigateToBack -> navigateToBack()
            }
        }
    }

    AudioEditScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEditScreen(
    navigateToBack: () -> Unit,
    data: AudioEditUiState,
    sendIntent: (AudioEditIntent) -> Unit,
) {
    var isShowSaveDialog by remember { mutableStateOf(false) }
    var isShowExitDialog by remember { mutableStateOf(false) }

    val onBackPressed = {
        if (data.isDirty) isShowExitDialog = true else navigateToBack()
    }
    BackHandler(enabled = data.isDirty) { isShowExitDialog = true }

    AudioEditSaveDialog(
        isShow = isShowSaveDialog,
        resultPlayTime = data.resultDurationMs.milliseconds.to_hh_colon_mm_colon_ss(),
        defaultNewMediaName = "${data.audioMediaName} (편집본)",
        hasEditedAudio = data.hasEditedAudio,
        hasSyncedLyrics = data.hasSyncedLyrics,
        onDismiss = { isShowSaveDialog = false },
        onConfirmOverwrite = { sendIntent(AudioEditIntent.OnConfirmOverwrite) },
        onConfirmSaveAsNew = { name -> sendIntent(AudioEditIntent.OnConfirmSaveAsNew(name)) },
    )
    AudioEditExitDialog(
        isShow = isShowExitDialog,
        onDismiss = { isShowExitDialog = false },
        onConfirmExit = navigateToBack,
    )

    val durationSummary = "원본 ${data.originDurationMs.milliseconds.to_hh_colon_mm_colon_ss()} " +
        "→ 편집 결과 ${data.resultDurationMs.milliseconds.to_hh_colon_mm_colon_ss()}"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "음원 편집") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로 가기",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = data.canSave,
                    onClick = { isShowSaveDialog = true },
                ) {
                    Text(text = if (data.isSaving) "저장 중..." else "편집한 음원 저장")
                }
            }
        },
    ) { innerPadding ->
        if (data.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        modifier = Modifier.padding(top = 12.dp),
                        text = "음원 정보를 불러오고 있습니다...",
                    )
                }
            }
            return@Scaffold
        }

        val viewport = rememberTimelineViewportState()
        SideEffect {
            viewport.updateContent(maxOf(data.originDurationMs, data.resultDurationMs))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = data.audioMediaName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                text = durationSummary,
                fontSize = 15.sp,
            )

            TimelineRulerView(
                modifier = Modifier.padding(top = 16.dp),
                viewport = viewport,
                baselineMs = data.baselineMs,
                endMs = data.resultDurationMs,
                onBaselineChanged = { ms -> sendIntent(AudioEditIntent.OnBaselineChanged(ms)) },
                onEndChanged = { ms -> sendIntent(AudioEditIntent.OnTimelineEndChanged(ms)) },
            )
            TimelineWaveformView(
                modifier = Modifier,
                viewport = viewport,
                waveform = data.waveform,
                timeline = data.timeline,
                selectedClipIndex = data.selectedClipIndex,
                baselineMs = data.baselineMs,
                previewPositionMs = data.previewPositionMs,
                onClipSelected = { index -> sendIntent(AudioEditIntent.OnClipSelected(index)) },
                onClipEdgeDragged = { index, edge, ms ->
                    sendIntent(AudioEditIntent.OnClipEdgeDragged(index = index, edge = edge, ms = ms))
                },
                onClipMoved = { index, positionMs ->
                    sendIntent(AudioEditIntent.OnClipMoved(index = index, positionMs = positionMs))
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "기준선 ${data.baselineMs.milliseconds.to_hh_colon_mm_colon_ss_dot_sss()}",
                    fontSize = 15.sp,
                )
                data.waveformLoadingPercent?.let { percent ->
                    Text(
                        text = "파형 불러오는 중 $percent%",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = data.canSplit,
                    onClick = { sendIntent(AudioEditIntent.OnClickSplit) },
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCut, contentDescription = null)
                    Text(modifier = Modifier.padding(start = 4.dp), text = "자르기")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = data.canDelete,
                    onClick = { sendIntent(AudioEditIntent.OnClickDeleteClip) },
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null)
                    Text(modifier = Modifier.padding(start = 4.dp), text = "삭제")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { sendIntent(AudioEditIntent.OnClickTogglePreview) },
                ) {
                    Icon(
                        imageVector = if (data.isPreviewPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                    )
                    Text(modifier = Modifier.padding(start = 4.dp), text = "미리듣기")
                }
            }

            if (!data.timeline.hasAudio) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "남은 소리가 없어 저장할 수 없습니다. 조각 경계를 꾹 눌러 끌어 되살려주세요.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioEditScreenPreview() {
    JellyTubeTheme {
        AudioEditScreen(
            navigateToBack = {},
            data = AudioEditUiState.getMock(),
            sendIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AudioEditScreenLoadingPreview() {
    JellyTubeTheme {
        AudioEditScreen(
            navigateToBack = {},
            data = AudioEditUiState.getEmpty(audioMediaId = 1),
            sendIntent = {},
        )
    }
}
