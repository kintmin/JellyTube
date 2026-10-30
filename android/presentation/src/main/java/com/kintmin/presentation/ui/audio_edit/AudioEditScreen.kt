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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
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
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_edit.dialog.AudioEditExitDialog
import com.kintmin.presentation.ui.audio_edit.dialog.AudioEditSaveDialog
import com.kintmin.presentation.ui.audio_edit.waveform.AudioWaveformView
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

/** 미세조정 단위. 핸들 드래그로는 잡기 어려운 짧은 간격을 버튼으로 맞춘다. */
private const val FINE_TUNE_STEP_MS = 500L

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
        hasEditedAudio = data.hasEditedAudio,
        hasSyncedLyrics = data.hasSyncedLyrics,
        onDismiss = { isShowSaveDialog = false },
        onConfirmOverwrite = { sendIntent(AudioEditIntent.OnConfirmOverwrite) },
    )
    AudioEditExitDialog(
        isShow = isShowExitDialog,
        onDismiss = { isShowExitDialog = false },
        onConfirmExit = navigateToBack,
    )

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
                        text = "파형을 읽고 있습니다...",
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = data.audioMediaName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                text = "원본 ${data.originDurationMs.milliseconds.to_hh_colon_mm_colon_ss()} " +
                    "→ 편집 결과 ${data.resultDurationMs.milliseconds.to_hh_colon_mm_colon_ss()}",
                fontSize = 15.sp,
            )

            AudioWaveformView(
                modifier = Modifier.padding(vertical = 16.dp),
                waveform = data.waveform,
                durationMs = data.originDurationMs,
                selectionStartMs = data.selectionStartMs,
                selectionEndMs = data.selectionEndMs,
                cutRanges = data.cutRanges,
                previewPositionMs = data.previewPositionMs,
                onSelectionChanged = { startMs, endMs ->
                    sendIntent(AudioEditIntent.OnSelectionChanged(startMs, endMs))
                },
            )

            SelectionRangeRow(
                label = "자를 구간 시작",
                valueMs = data.selectionStartMs,
                onChange = { newStartMs ->
                    sendIntent(
                        AudioEditIntent.OnSelectionChanged(
                            startMs = newStartMs.coerceAtMost(data.selectionEndMs),
                            endMs = data.selectionEndMs,
                        )
                    )
                },
            )
            SelectionRangeRow(
                label = "자를 구간 끝",
                valueMs = data.selectionEndMs,
                onChange = { newEndMs ->
                    sendIntent(
                        AudioEditIntent.OnSelectionChanged(
                            startMs = data.selectionStartMs,
                            endMs = newEndMs.coerceAtLeast(data.selectionStartMs),
                        )
                    )
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = data.hasSelection,
                    onClick = { sendIntent(AudioEditIntent.OnClickCutSelection) },
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCut, contentDescription = null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "선택 구간 자르기",
                    )
                }
                OutlinedButton(
                    onClick = { sendIntent(AudioEditIntent.OnClickTogglePreview) },
                ) {
                    Icon(
                        imageVector = if (data.isPreviewPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = "미리듣기",
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = "미리듣기",
                    )
                }
            }

            if (data.cutRanges.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "잘라낸 구간",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedButton(onClick = { sendIntent(AudioEditIntent.OnClickResetCuts) }) {
                        Text(text = "전체 되살리기")
                    }
                }

                data.cutRanges.forEachIndexed { index, cutRange ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "${cutRange.startMs.milliseconds.to_hh_colon_mm_colon_ss()} ~ " +
                                cutRange.endMs.milliseconds.to_hh_colon_mm_colon_ss(),
                            fontSize = 16.sp,
                        )
                        IconButton(
                            onClick = { sendIntent(AudioEditIntent.OnClickRestoreCut(index)) },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "이 구간 되살리기",
                            )
                        }
                    }
                }
            }

            if (data.isDirty && data.keepSegments.isEmpty()) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "남는 구간이 없어 저장할 수 없습니다. 잘라낸 구간을 일부 되살려주세요.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** 선택 구간의 한쪽 경계를 수치로 미세조정하는 줄. */
@Composable
private fun SelectionRangeRow(
    label: String,
    valueMs: Long,
    onChange: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "$label  ${valueMs.milliseconds.to_hh_colon_mm_colon_ss()}",
            fontSize = 16.sp,
        )
        OutlinedButton(onClick = { onChange(valueMs - FINE_TUNE_STEP_MS) }) {
            Text(text = "-0.5초")
        }
        OutlinedButton(
            modifier = Modifier.padding(start = 8.dp),
            onClick = { onChange(valueMs + FINE_TUNE_STEP_MS) },
        ) {
            Text(text = "+0.5초")
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
