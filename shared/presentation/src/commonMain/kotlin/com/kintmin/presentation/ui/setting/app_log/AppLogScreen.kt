package com.kintmin.presentation.ui.setting.app_log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.platform.AppLogShareResult
import com.kintmin.presentation.platform.rememberAppLogSharer
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.EmptyStateView
import com.kintmin.presentation.ui.common.FloatingSearchFieldHeight
import com.kintmin.presentation.ui.setting.app_log.date_selector.AppLogDateSelectorView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppLogScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<AppLogViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val toaster = rememberToaster()
    val appLogSharer = rememberAppLogSharer()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(AppLogIntent.OnInit)
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AppLogEvent.ShareLogFiles -> when (appLogSharer.share(event.fileFullPaths)) {
                    AppLogShareResult.Started -> Unit
                    AppLogShareResult.NoLogFiles -> toaster.show("전송할 로그 파일이 없습니다.")
                    AppLogShareResult.NoShareTarget -> toaster.show("메일 앱을 찾을 수 없습니다.")
                    AppLogShareResult.Failed -> toaster.show("로그 전송을 시작하지 못했습니다.")
                }
            }
        }
    }

    AppLogScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogScreen(
    uiState: AppLogUiState,
    navigateToBack: () -> Unit,
    sendIntent: (AppLogIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = "앱 로그",
                onClickBack = navigateToBack,
                actions = {
                    IconButton(
                        onClick = { sendIntent(AppLogIntent.OnClickShare) },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "로그 전송",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AppLogContentView(uiState = uiState, sendIntent = sendIntent)

            if (uiState.logDateList.isNotEmpty()) {
                AppLogDateSelectorView(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    dateList = uiState.logDateList,
                    selectedDate = uiState.selectedLogDate,
                    onSelectDate = { date -> sendIntent(AppLogIntent.OnClickLogDate(date)) },
                )
            }
        }
    }
}

@Composable
private fun AppLogContentView(
    uiState: AppLogUiState,
    sendIntent: (AppLogIntent) -> Unit,
) {
    // 날짜 선택이 목록 위에 떠 있어 그만큼 비켜 둔다.
    val topInset = if (uiState.logDateList.isNotEmpty()) FloatingSearchFieldHeight else 0.dp

    if (uiState.isLoading && uiState.logLineList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.logLineList.isEmpty()) {
        AppLogEmptyView(
            modifier = Modifier.padding(top = topInset),
            hasLogDate = uiState.logDateList.isNotEmpty(),
        )
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(uiState.selectedLogDate) { listState.scrollToItem(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = topInset),
        verticalArrangement = Arrangement.Top,
    ) {
        itemsIndexed(
            items = uiState.logLineList,
        ) { index, line ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            AppLogLineView(line = line)

            if (index >= uiState.logLineList.lastIndex - 5 && uiState.hasNextPage) {
                LaunchedEffect(uiState.logLineList.size) {
                    sendIntent(AppLogIntent.OnRequestNextPage)
                }
            }
        }

        if (uiState.hasNextPage) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun AppLogLineView(line: AppLogLineUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (line.time != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = line.time,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = contentMuted,
                )
                line.type?.let { type ->
                    Text(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        text = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                line.tag?.let { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Text(
            text = line.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppLogEmptyView(
    hasLogDate: Boolean,
    modifier: Modifier = Modifier,
) {
    EmptyStateView(
        modifier = modifier.fillMaxSize(),
        icon = Icons.Rounded.Description,
        title = if (hasLogDate) "이 날의 로그가 없습니다" else "남은 로그가 없습니다",
        description = "앱을 쓰는 동안 남은 기록이 날짜별로 여기에 모입니다",
    )
}

@Preview
@Composable
private fun AppLogLineViewPreview() {
    JellyTubeTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            AppLogLineView(
                line = parseAppLogLine("[2026-11-21 09:05:01.042] [SENTRY_EVENT] download 완료"),
            )
            AppLogLineView(line = parseAppLogLine("[2026-11-21 09:05:02.100] [DEBUG] [DeepLink] 받음: jellytube://a"))
            AppLogLineView(line = parseAppLogLine("    at com.kintmin.Foo.bar(Foo.kt:12)"))
        }
    }
}

@Preview
@Composable
private fun AppLogContentViewPreview() {
    JellyTubeTheme {
        AppLogContentView(
            uiState = AppLogUiState(
                logLineList = listOf(parseAppLogLine("[2026-11-22 09:05:02.100] [DEBUG] [DeepLink] 받음: jellytube://a")),
            ),
            sendIntent = {},
        )
    }
}

@Preview
@Composable
private fun AppLogEmptyViewPreview() {
    JellyTubeTheme {
        AppLogEmptyView(hasLogDate = true)
    }
}

@Preview
@Composable
fun AppLogScreenPreview() {
    JellyTubeTheme {
        AppLogScreen(
            uiState = AppLogUiState(
                logDateList = listOf("2026-11-22", "2026-11-21"),
                selectedLogDate = "2026-11-22",
                logLineList = listOf(parseAppLogLine("[2026-11-22 09:05:02.100] [DEBUG] [DeepLink] 받음: jellytube://a")),
            ),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
