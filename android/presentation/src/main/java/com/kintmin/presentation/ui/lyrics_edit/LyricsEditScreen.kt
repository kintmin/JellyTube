package com.kintmin.presentation.ui.lyrics_edit

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kintmin.presentation.remember_state.rememberReorderState
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.wheel_picker.TimePickerBottomSheet
import com.kintmin.presentation.ui.lyrics_edit.dialog.LyricsEditExitDialog
import com.kintmin.presentation.ui.lyrics_edit.list_item.LyricsEditItemView
import org.koin.androidx.compose.koinViewModel

/** 드래그 재정렬 시 "몇 칸 이동" 계산의 기준이 되는 한 행의 근사 높이. */
private val REORDER_ITEM_HEIGHT = 112.dp

@Composable
fun LyricsEditScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<LyricsEditViewModel>()
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LyricsEditEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                LyricsEditEvent.NavigateToBack -> navigateToBack()
            }
        }
    }

    LyricsEditScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditScreen(
    navigateToBack: () -> Unit,
    data: LyricsEditUiState,
    sendIntent: (LyricsEditIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderState(
        listState = listState,
        dataList = data.rows,
        idOf = { it.id },
        initializeItemHeightPx = REORDER_ITEM_HEIGHT,
    )
    // 드래그 중이 아닐 때만 외부 상태(rows)를 표시 리스트에 반영한다.
    LaunchedEffect(data.rows) {
        if (reorderState.draggingItemId == null) {
            reorderState.items.clear()
            reorderState.items.addAll(data.rows)
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }
    val onBackPressed = {
        if (data.isDirty) showExitDialog = true else navigateToBack()
    }

    // 수정사항이 있으면 기기 뒤로가기를 가로채 확인 다이얼로그를 띄운다.
    BackHandler(enabled = data.isDirty) { showExitDialog = true }

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
                title = {
                    Text(
                        text = data.title.ifBlank { "가사 수정" },
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "ArrowBackIosNew",
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
                    enabled = !data.isSaving,
                    onClick = { sendIntent(LyricsEditIntent.OnClickSave) },
                ) {
                    Text(text = if (data.isSaving) "저장 중..." else "수정사항 저장")
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
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = { sendIntent(LyricsEditIntent.OnSplitByNewline) },
            ) {
                Text("줄넘김 기준으로 가사 쪼개기")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                itemsIndexed(
                    items = reorderState.items,
                    key = { _, item -> item.id },
                ) { _, row ->
                    LyricsEditItemView(
                        modifier = Modifier
                            .animateItem()
                            .zIndex(1f.takeIf { row.id == reorderState.draggingItemId } ?: 0f),
                        row = row,
                        draggingItemId = reorderState.draggingItemId,
                        onClickTime = { pickerRowId = it },
                        onChangeText = { id, text -> sendIntent(LyricsEditIntent.OnChangeText(id, text)) },
                        onAddRowBelow = { sendIntent(LyricsEditIntent.OnAddRowBelow(it)) },
                        onDeleteRow = { sendIntent(LyricsEditIntent.OnDeleteRow(it)) },
                        onDragStart = reorderState::onDragStart,
                        onDrag = reorderState::onDrag,
                        onDragEnd = {
                            if (reorderState.getDraggingItemIndex() != null) {
                                sendIntent(LyricsEditIntent.OnReorder(reorderState.items.map { it.id }))
                            }
                            reorderState.onDragEnd()
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LyricsEditScreenPreview() {
    JellyTubeTheme {
        LyricsEditScreen(
            navigateToBack = {},
            data = LyricsEditUiState.getMock(),
            sendIntent = {},
        )
    }
}
