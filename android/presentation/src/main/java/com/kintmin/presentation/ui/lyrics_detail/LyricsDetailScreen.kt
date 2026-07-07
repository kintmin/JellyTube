package com.kintmin.presentation.ui.lyrics_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.kintmin.presentation.theme.JellyTubeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LyricsDetailScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<LyricsDetailViewModel>()
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is LyricsDetailEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

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
                title = {
                    Text(
                        text = listOf(data.trackName, data.artistName)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "가사" },
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
                    enabled = !data.isApplying,
                    onClick = { sendIntent(LyricsDetailIntent.OnClickApply) },
                ) {
                    Text(text = if (data.isApplying) "적용 중..." else "현재 음원에 해당 가사 적용")
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
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

@Preview(showBackground = true)
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
