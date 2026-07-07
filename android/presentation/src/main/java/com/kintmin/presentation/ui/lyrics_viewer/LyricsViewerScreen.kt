package com.kintmin.presentation.ui.lyrics_viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.lyrics_viewer.dialog.LyricsDeleteDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LyricsViewerScreen(
    navigateToBack: () -> Unit,
    navigateToLyricsEdit: () -> Unit,
) {
    val viewModel = koinViewModel<LyricsViewerViewModel>()
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LyricsViewerEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                LyricsViewerEvent.NavigateToBack -> navigateToBack()
            }
        }
    }

    LyricsViewerScreen(
        navigateToBack = navigateToBack,
        navigateToLyricsEdit = navigateToLyricsEdit,
        data = data,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsViewerScreen(
    navigateToBack: () -> Unit,
    navigateToLyricsEdit: () -> Unit,
    data: LyricsViewerUiState,
    sendIntent: (LyricsViewerIntent) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LyricsDeleteDialog(
        isShow = showDeleteDialog,
        audioMediaName = data.title,
        onDismiss = { showDeleteDialog = false },
        onConfirmDelete = { sendIntent(LyricsViewerIntent.OnClickDeleteLyrics) },
    )

    // 재생 위치는 Flow가 없으므로 300ms 폴링으로 현재 가사 줄을 갱신한다.
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            lifecycleOwner.lifecycleScope.launch {
                while (isActive) {
                    delay(300)
                    sendIntent(LyricsViewerIntent.OnRefreshPosition)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = data.title.ifBlank { "가사" },
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "ArrowBackIosNew",
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "더보기",
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (data.showTranslateMenu) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (data.translationLines != null) "다시 번역하기" else "번역파일 만들기")
                                },
                                onClick = {
                                    menuExpanded = false
                                    sendIntent(LyricsViewerIntent.OnClickCreateTranslation)
                                },
                            )
                        }
                        if (data.showTransliterateMenu) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (data.transliterationLines != null) "다시 음차 번역하기" else "음차 번역파일 만들기")
                                },
                                onClick = {
                                    menuExpanded = false
                                    sendIntent(LyricsViewerIntent.OnClickCreateTransliteration)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("편집하기") },
                            onClick = {
                                menuExpanded = false
                                navigateToLyricsEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("삭제하기") },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            data.isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            data.lines.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "표시할 가사가 없습니다.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    itemsIndexed(data.lines) { index, line ->
                        val isActiveLine = data.isSynced && index == data.activeIndex
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = line.ifBlank { " " },
                                fontSize = if (isActiveLine) 20.sp else 16.sp,
                                lineHeight = if (isActiveLine) 28.sp else 24.sp,
                                fontWeight = if (isActiveLine) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActiveLine) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            // 원본 → 음차 → 번역 순서로 작은 글씨로 덧붙인다.
                            data.transliterationLines?.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { transliteration ->
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = transliteration,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            data.translationLines?.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { translation ->
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = translation,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LyricsViewerScreenPreview() {
    JellyTubeTheme {
        LyricsViewerScreen(
            navigateToBack = {},
            navigateToLyricsEdit = {},
            data = LyricsViewerUiState.getMock(),
            sendIntent = {},
        )
    }
}
