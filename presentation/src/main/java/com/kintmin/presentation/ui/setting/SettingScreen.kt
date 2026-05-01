package com.kintmin.presentation.ui.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun SettingScreen(
    navigateToBack: () -> Unit,
    navigateToStep: () -> Unit,
    navigateToAppLog: () -> Unit,
) {
    val viewModel = hiltViewModel<SettingViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(SettingIntent.OnInit)
    }
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SettingEvent.NavigateToStepScreen -> navigateToStep()
                SettingEvent.NavigateToAppLogScreen -> navigateToAppLog()
            }
        }
    }

    SettingScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    uiState: SettingUiState,
    navigateToBack: () -> Unit,
    sendIntent: (SettingIntent) -> Unit,
) {
    if (uiState.isPlaylistIdOnDownloadBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                sendIntent(SettingIntent.OnDismissPlaylistIdOnDownloadBottomSheet)
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = uiState.selectablePlaylistList,
                    key = { item -> item.id },
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                sendIntent(SettingIntent.OnSelectPlaylistIdOnDownload(item.id))
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.name,
                            color = if (item.isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        sendIntent(SettingIntent.OnClickShouldInsertAtTopOnDownloadTile)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "다운로드 시 맨 위에 추가",
                )
                Switch(
                    checked = uiState.shouldInsertAtTopOnDownload,
                    onCheckedChange = { isChecked ->
                        sendIntent(SettingIntent.OnToggleShouldInsertAtTopOnDownload(isChecked))
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        sendIntent(SettingIntent.OnClickPlaylistIdOnDownloadTile)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "다운로드 시 재생목록",
                )
                Text(
                    text = uiState.playlistIdOnDownloadName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        sendIntent(SettingIntent.OnClickStepTile)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "걸음수 현황 보기")
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "걸음수 현황 화면으로 이동",
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        sendIntent(SettingIntent.OnClickAppLogTile)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "앱 로그 보기")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingScreenPreview() {
    JellyTubeTheme {
        SettingScreen(
            uiState = SettingUiState(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}