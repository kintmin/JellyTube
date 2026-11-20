package com.kintmin.presentation.ui.lyrics_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.ui.common.wheel_picker.TimePickerBottomSheet
import com.kintmin.presentation.ui.lyrics_edit.dialog.LyricsEditExitDialog
import com.kintmin.presentation.ui.lyrics_edit.list_item.LyricsEditItemView
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LyricsEditScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<LyricsEditViewModel>()
    val data by viewModel.data.collectAsState()
    val toaster = rememberToaster()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LyricsEditEvent.ShowToast ->
                    toaster.show(event.message)

                LyricsEditEvent.NavigateToBack -> navigateToBack()

                // 되돌리기는 마지막 삭제 한 건만 가능하므로 이전 스낵바는 걷어낸다.
                LyricsEditEvent.ShowUndoDeleteRow -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "줄을 삭제했어요",
                        actionLabel = "되돌리기",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.sendIntent(LyricsEditIntent.OnUndoDeleteRow)
                    }
                }
            }
        }
    }

    LyricsEditScreen(
        navigateToBack = navigateToBack,
        data = data,
        snackbarHostState = snackbarHostState,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditScreen(
    navigateToBack: () -> Unit,
    data: LyricsEditUiState,
    snackbarHostState: SnackbarHostState,
    sendIntent: (LyricsEditIntent) -> Unit,
) {
    val listState = rememberLazyListState()

    var showExitDialog by remember { mutableStateOf(false) }
    val onBackPressed = {
        if (data.isDirty) showExitDialog = true else navigateToBack()
    }

    // 수정사항이 있으면 기기 뒤로가기를 가로채 확인 다이얼로그를 띄운다.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = data.isDirty,
    ) {
        showExitDialog = true
    }

    LyricsEditExitDialog(
        isShow = showExitDialog,
        onDismiss = { showExitDialog = false },
        onConfirmExit = navigateToBack,
    )

    var pickerRowId by remember { mutableStateOf<Int?>(null) }
    val pickerRow = data.rows.firstOrNull { it.id == pickerRowId }
    if (pickerRow != null) {
        TimePickerBottomSheet(
            initialTimeMs = pickerRow.timeMs,
            audioDurationMs = data.durationMs.takeIf { it > 0 },
            onConfirm = { timeMs ->
                sendIntent(LyricsEditIntent.OnChangeTime(pickerRow.id, timeMs))
                pickerRowId = null
            },
            onDismissRequest = { pickerRowId = null },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        text = data.title.ifBlank { "가사 편집" },
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "뒤로",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    enabled = data.isDirty && !data.isSaving,
                    onClick = { sendIntent(LyricsEditIntent.OnClickSave) },
                ) {
                    Text(text = if (data.isSaving) "저장 중…" else "저장")
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
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (data.canSplit) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = { sendIntent(LyricsEditIntent.OnSplitByNewline) },
                ) {
                    Text("줄넘김 기준으로 가사 쪼개기")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                itemsIndexed(
                    items = data.rows,
                    key = { _, item -> item.id },
                ) { _, row ->
                    LyricsEditItemView(
                        modifier = Modifier.animateItem(),
                        row = row,
                        showTranslation = data.hasTranslation,
                        showTransliteration = data.hasTransliteration,
                        onClickTime = { pickerRowId = it },
                        onChangeText = { id, text -> sendIntent(LyricsEditIntent.OnChangeText(id, text)) },
                        onChangeTranslation = { id, text -> sendIntent(LyricsEditIntent.OnChangeTranslation(id, text)) },
                        onChangeTransliteration = { id, text -> sendIntent(LyricsEditIntent.OnChangeTransliteration(id, text)) },
                        onAddRowBelow = { sendIntent(LyricsEditIntent.OnAddRowBelow(it)) },
                        onDeleteRow = { sendIntent(LyricsEditIntent.OnDeleteRow(it)) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LyricsEditScreenPreview() {
    JellyTubeTheme {
        LyricsEditScreen(
            navigateToBack = {},
            data = LyricsEditUiState.getMock(),
            snackbarHostState = remember { SnackbarHostState() },
            sendIntent = {},
        )
    }
}
