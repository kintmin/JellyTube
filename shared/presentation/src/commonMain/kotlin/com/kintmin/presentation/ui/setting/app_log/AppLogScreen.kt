package com.kintmin.presentation.ui.setting.app_log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.kintmin.presentation.theme.contentDisabled
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.contentSubtle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowBackIosNew
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import com.kintmin.presentation.platform.AppLogShareResult
import com.kintmin.presentation.platform.rememberAppLogSharer
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.ui.setting.SettingScreen
import com.kintmin.presentation.ui.setting.SettingUiState

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
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        text = "앱 로그",
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "뒤로가기",
                        )
                    }
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.logDateList,
                    key = { date -> date },
                ) { date ->
                    val isSelected = date == uiState.selectedLogDate
                    FilterChip(
                        selected = isSelected,
                        onClick = { sendIntent(AppLogIntent.OnClickLogDate(date)) },
                        label = { Text(text = date) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            if (uiState.isLoading && uiState.logLineList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            if (uiState.logLineList.isEmpty()) {
                AppLogEmptyView(hasLogDate = uiState.logDateList.isNotEmpty())
                return@Scaffold
            }

            val listState = rememberLazyListState()
            LaunchedEffect(uiState.selectedLogDate) { listState.scrollToItem(0) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
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
private fun AppLogEmptyView(hasLogDate: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = contentDisabled,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = if (hasLogDate) "이 날의 로그가 없습니다" else "남은 로그가 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = contentSubtle,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "앱을 쓰는 동안 남은 기록이 날짜별로 여기에 모입니다",
            style = MaterialTheme.typography.bodySmall,
            color = contentMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun AppLogLineViewPreview() {
    JellyTubeTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            AppLogLineView(
                line = parseAppLogLine("[2026-11-21 09:05:01.042] [FIREBASE_EVENT] download 완료"),
            )
            AppLogLineView(line = parseAppLogLine("    at com.kintmin.Foo.bar(Foo.kt:12)"))
        }
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
            uiState =  AppLogUiState(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
