package com.kintmin.presentation.ui.setting.app_log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import com.kintmin.presentation.platform.AppLogShareResult
import com.kintmin.presentation.platform.rememberAppLogSharer
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.SettingScreen
import com.kintmin.presentation.ui.setting.SettingUiState

@Composable
fun AppLogScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<AppLogViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(AppLogIntent.OnInit)
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
    val toaster = rememberToaster()
    val appLogSharer = rememberAppLogSharer()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("앱 로그") },
                navigationIcon = {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            when (appLogSharer.share()) {
                                AppLogShareResult.Started -> Unit
                                AppLogShareResult.NoLogFiles ->
                                    toaster.show("전송할 로그 파일이 없습니다.")
                                AppLogShareResult.NoShareTarget ->
                                    toaster.show("메일 앱을 찾을 수 없습니다.")
                                AppLogShareResult.Failed ->
                                    toaster.show("로그 전송을 시작하지 못했습니다.")
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                items(
                    items = uiState.logDateList,
                    key = { date -> date },
                ) { date ->
                    val isSelected = date == uiState.selectedLogDate
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                sendIntent(AppLogIntent.OnClickLogDate(date))
                            },
                        text = date,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
            ) {
                itemsIndexed(
                    items = uiState.logLineList,
                ) { index, line ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                    )

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
