package com.kintmin.presentation.ui.setting

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.platform.service.StepForegroundService
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DownloadPlaylistSelectorBottomSheet

@Composable
fun SettingScreen(
    navigateToBack: () -> Unit,
    navigateToStep: () -> Unit,
    navigateToAppLog: () -> Unit,
    navigateToShare: () -> Unit,
    navigateToFileShareReceive: () -> Unit,
) {
    val viewModel = hiltViewModel<SettingViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.sendIntent(SettingIntent.OnActivityRecognitionGranted)
            StepForegroundService.startService(context)
        } else {
            viewModel.sendIntent(SettingIntent.OnActivityRecognitionDenied)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sendIntent(SettingIntent.OnInit)
    }
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SettingEvent.NavigateToStepScreen -> navigateToStep()
                SettingEvent.NavigateToAppLogScreen -> navigateToAppLog()
                SettingEvent.NavigateToShareScreen -> navigateToShare()
                SettingEvent.NavigateToFileShareReceiveScreen -> navigateToFileShareReceive()
                SettingEvent.StopStepForegroundService -> StepForegroundService.stopService(context)
                SettingEvent.RequestActivityRecognitionPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val permission = Manifest.permission.ACTIVITY_RECOGNITION
                        val alreadyGranted = ContextCompat.checkSelfPermission(context, permission) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (alreadyGranted) {
                            viewModel.sendIntent(SettingIntent.OnActivityRecognitionGranted)
                            StepForegroundService.startService(context)
                        } else {
                            activityRecognitionLauncher.launch(permission)
                        }
                    } else {
                        viewModel.sendIntent(SettingIntent.OnActivityRecognitionGranted)
                        StepForegroundService.startService(context)
                    }
                }
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
        DownloadPlaylistSelectorBottomSheet(
            playlistList = uiState.selectablePlaylistList,
            onSelectPlaylist = { playlistId ->
                sendIntent(SettingIntent.OnSelectPlaylistIdOnDownload(playlistId))
            },
            onDismissRequest = {
                sendIntent(SettingIntent.OnDismissPlaylistIdOnDownloadBottomSheet)
            },
        )
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
                        sendIntent(SettingIntent.OnToggleIsStepEnabled(!uiState.isStepEnabled))
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "걸음수 이용",
                )
                Switch(
                    checked = uiState.isStepEnabled,
                    onCheckedChange = { isChecked ->
                        sendIntent(SettingIntent.OnToggleIsStepEnabled(isChecked))
                    },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        sendIntent(SettingIntent.OnClickShareTile)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Quick Share로 공유받기")
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Quick Share 화면으로 이동",
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        sendIntent(SettingIntent.OnClickFileShareReceiveTile)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "파일 공유 받기")
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "파일 공유 받기 화면으로 이동",
                )
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
