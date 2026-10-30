package com.kintmin.presentation.ui.audio_edit

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed100
import com.kintmin.presentation.ui.audio_edit.controls.AudioEditControlsView
import com.kintmin.presentation.ui.audio_edit.dialog.AudioEditExitDialog
import com.kintmin.presentation.ui.audio_edit.dialog.AudioEditSaveDialog
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineGrab
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineRulerView
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineWaveformView
import com.kintmin.presentation.ui.audio_edit.waveform.rememberTimelineViewportState
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

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

    val durationSummary = data.durationSummary()

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
        var grab by remember { mutableStateOf<TimelineGrab?>(null) }
        SideEffect {
            viewport.updateContent(maxOf(data.originDurationMs, data.resultDurationMs))
        }
        LaunchedEffect(data.isPreviewPlaying) {
            if (data.isPreviewPlaying) viewport.resumeFollow()
        }
        LaunchedEffect(data.previewPositionMs) {
            data.previewPositionMs?.let(viewport::follow)
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
                grab = grab,
                baselineMs = data.baselineMs,
                previewPositionMs = data.previewPositionMs,
                onClipSelected = { index -> sendIntent(AudioEditIntent.OnClipSelected(index)) },
                onClipEdgeDragged = { index, edge, ms ->
                    sendIntent(AudioEditIntent.OnClipEdgeDragged(index = index, edge = edge, ms = ms))
                },
                onClipMoved = { index, positionMs ->
                    sendIntent(AudioEditIntent.OnClipMoved(index = index, positionMs = positionMs))
                },
                onGrabChanged = { grab = it },
            )

            AudioEditControlsView(data = data, grab = grab, sendIntent = sendIntent)
        }
    }
}

/**
 * "원본 5:00 → 결과 5:10".
 * 결과 시간은 끝 표시(빨간 손잡이)와 같은 색으로 칠해, 그걸 끌면 이 값이 바뀐다는 연결을 보여준다.
 */
private fun AudioEditUiState.durationSummary(): AnnotatedString = buildAnnotatedString {
    append("원본 ${originDurationMs.milliseconds.to_hh_colon_mm_colon_ss()} → 결과 ")
    withStyle(SpanStyle(color = bloodyRed100, fontWeight = FontWeight.Bold)) {
        append(resultDurationMs.milliseconds.to_hh_colon_mm_colon_ss())
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
