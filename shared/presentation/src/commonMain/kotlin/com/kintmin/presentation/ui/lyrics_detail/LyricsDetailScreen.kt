package com.kintmin.presentation.ui.lyrics_detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LyricsDetailScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<LyricsDetailViewModel>()
    val data by viewModel.uiState.collectAsState()
    val toaster = rememberToaster()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LyricsDetailEvent.ShowToast ->
                    toaster.show(event.message)

                LyricsDetailEvent.NavigateToBack -> navigateToBack()
            }
        }
    }

    LyricsDetailScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsDetailScreen(
    navigateToBack: () -> Unit,
    data: LyricsDetailUiState,
    sendIntent: (LyricsDetailIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        text = listOf(data.trackName, data.artistName)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "가사" },
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "뒤로",
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomActionBar {
                BottomActionButton(
                    text = if (data.isApplying) "적용 중…" else "현재 음원에 해당 가사 적용",
                    enabled = !data.isApplying,
                    onClick = { sendIntent(LyricsDetailIntent.OnClickApply) },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + innerPadding.calculateBottomPadding(),
            ),
        ) {
            item(key = "lyrics") {
                Text(
                    text = data.displayLyrics.ifBlank { "표시할 가사가 없습니다." },
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LyricsDetailScreenPreview() {
    JellyTubeTheme {
        LyricsDetailScreen(
            navigateToBack = {},
            data = LyricsDetailUiState.getMock(),
            sendIntent = {},
        )
    }
}
