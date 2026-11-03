package com.kintmin.presentation.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.kintmin.presentation.platform.AppPermission
import com.kintmin.presentation.platform.rememberAppVersionName
import com.kintmin.presentation.platform.rememberAudioFilePicker
import com.kintmin.presentation.platform.rememberPermissionController
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DownloadPlaylistSelectorBottomSheet
import com.kintmin.presentation.ui.setting.section.SettingItemView
import com.kintmin.presentation.ui.setting.section.SettingNavigateIcon
import com.kintmin.presentation.ui.setting.section.SettingSectionView


@Composable
fun SettingScreen(
    navigateToBack: () -> Unit,
    navigateToStep: () -> Unit,
    navigateToAppLog: () -> Unit,
    navigateToShare: () -> Unit,
    navigateToFileShareReceive: () -> Unit,
    navigateToDeviceSave: () -> Unit,
) {
    val viewModel = koinViewModel<SettingViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val toaster = rememberToaster()
    val appVersion = rememberAppVersionName()
    val permissionController = rememberPermissionController()
    // 음원 가져오기는 선택기를 "오디오" 루트에서 연다.
    val audioFilePicker = rememberAudioFilePicker(startAtAudioRoot = true) { fileIdentifiers ->
        viewModel.sendIntent(SettingIntent.OnAudioFilesSelected(fileIdentifiers))
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
                SettingEvent.NavigateToDeviceSaveScreen -> navigateToDeviceSave()
                SettingEvent.LaunchAudioFilePicker -> audioFilePicker.launch()
                is SettingEvent.ShowToast ->
                    toaster.show(event.message)
                SettingEvent.RequestActivityRecognitionPermission -> {
                    // 이 권한이 OS 버전별로 필요한지는 구현이 판단한다.
                    if (permissionController.isGranted(AppPermission.ActivityRecognition)) {
                        viewModel.sendIntent(SettingIntent.OnActivityRecognitionGranted)
                    } else {
                        permissionController.request(
                            listOf(AppPermission.ActivityRecognition),
                        ) { result ->
                            if (result[AppPermission.ActivityRecognition] == true) {
                                viewModel.sendIntent(SettingIntent.OnActivityRecognitionGranted)
                            } else {
                                viewModel.sendIntent(SettingIntent.OnActivityRecognitionDenied)
                            }
                        }
                    }
                }
            }
        }
    }

    SettingScreen(
        uiState = uiState,
        appVersion = appVersion,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    uiState: SettingUiState,
    appVersion: String,
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

    if (uiState.isImportPlaylistBottomSheetVisible) {
        DownloadPlaylistSelectorBottomSheet(
            playlistList = uiState.selectablePlaylistList,
            onSelectPlaylist = { playlistId ->
                sendIntent(SettingIntent.OnSelectImportPlaylist(playlistId))
            },
            onDismissRequest = {
                sendIntent(SettingIntent.OnDismissImportPlaylistBottomSheet)
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingSectionView(title = "다운로드", showDivider = false) {
                SettingItemView(
                    title = "다운로드 시 맨 위에 추가",
                    icon = Icons.Rounded.VerticalAlignTop,
                    onClick = { sendIntent(SettingIntent.OnClickShouldInsertAtTopOnDownloadTile) },
                    trailing = {
                        Switch(
                            checked = uiState.shouldInsertAtTopOnDownload,
                            onCheckedChange = { isChecked ->
                                sendIntent(SettingIntent.OnToggleShouldInsertAtTopOnDownload(isChecked))
                            },
                        )
                    },
                )
                SettingItemView(
                    title = "다운로드 시 재생목록",
                    icon = Icons.Rounded.LibraryMusic,
                    description = uiState.playlistIdOnDownloadName,
                    onClick = { sendIntent(SettingIntent.OnClickPlaylistIdOnDownloadTile) },
                )
            }

            SettingSectionView(title = "가져오기 및 내보내기") {
                SettingItemView(
                    title = "음원 파일 선택해서 가져오기",
                    icon = Icons.Rounded.AudioFile,
                    enabled = !uiState.isImporting,
                    onClick = { sendIntent(SettingIntent.OnClickImportAudioTile) },
                    trailing = if (uiState.isImporting) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        null
                    },
                )
                SettingItemView(
                    title = "Quick Share로 공유받기",
                    icon = Icons.Rounded.Share,
                    onClick = { sendIntent(SettingIntent.OnClickShareTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "Quick Share 화면으로 이동") },
                )
                SettingItemView(
                    title = "PC에서 공유받기",
                    icon = Icons.Rounded.Computer,
                    onClick = { sendIntent(SettingIntent.OnClickFileShareReceiveTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "PC에서 공유받기 화면으로 이동") },
                )
                SettingItemView(
                    title = "파일 기기에 저장하기",
                    icon = Icons.Rounded.SaveAlt,
                    onClick = { sendIntent(SettingIntent.OnClickDeviceSaveTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "기기에 저장하기 화면으로 이동") },
                )
            }

            SettingSectionView(title = "걸음수") {
                SettingItemView(
                    title = "걸음수 이용",
                    icon = Icons.Rounded.DirectionsWalk,
                    onClick = { sendIntent(SettingIntent.OnToggleIsStepEnabled(!uiState.isStepEnabled)) },
                    trailing = {
                        Switch(
                            checked = uiState.isStepEnabled,
                            onCheckedChange = { isChecked ->
                                sendIntent(SettingIntent.OnToggleIsStepEnabled(isChecked))
                            },
                        )
                    },
                )
                SettingItemView(
                    title = "걸음수 현황 보기",
                    icon = Icons.Rounded.BarChart,
                    onClick = { sendIntent(SettingIntent.OnClickStepTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "걸음수 현황 화면으로 이동") },
                )
            }

            SettingSectionView(title = "문제 해결") {
                SettingItemView(
                    title = "이상 데이터 점검 및 제거",
                    icon = Icons.Rounded.CleaningServices,
                    description = "음원 추가 오류 시 권장",
                    onClick = { sendIntent(SettingIntent.OnClickAnomalyDataCheckTile) },
                )
                SettingItemView(
                    title = "앱 로그 보기",
                    icon = Icons.Rounded.Description,
                    onClick = { sendIntent(SettingIntent.OnClickAppLogTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "앱 로그 화면으로 이동") },
                )
            }

            SettingSectionView(title = "앱 정보") {
                SettingItemView(
                    title = "버전",
                    icon = Icons.Rounded.Info,
                    description = appVersion,
                    onClick = null,
                )
            }
        }
    }
}

@Preview
@Composable
fun SettingScreenPreview() {
    JellyTubeTheme {
        SettingScreen(
            uiState = SettingUiState(),
            appVersion = "1.2.2",
            navigateToBack = {},
            sendIntent = {},
        )
    }
}

