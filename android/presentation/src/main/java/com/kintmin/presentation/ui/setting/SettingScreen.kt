package com.kintmin.presentation.ui.setting

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.koin.androidx.compose.koinViewModel
import com.kintmin.platform.service.StepForegroundService
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DownloadPlaylistSelectorBottomSheet
import com.kintmin.presentation.ui.setting.section.SettingItemView
import com.kintmin.presentation.ui.setting.section.SettingNavigateIcon
import com.kintmin.presentation.ui.setting.section.SettingSectionView

private class OpenMultipleAudioDocuments : ActivityResultContracts.OpenMultipleDocuments() {

    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, AUDIO_ROOT_URI)
        }

    private companion object {
        /** DocumentsUI의 "오디오" 루트. MediaDocumentsProvider가 제공한다. */
        val AUDIO_ROOT_URI: Uri =
            "content://com.android.providers.media.documents/root/audio_root".toUri()
    }
}

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
    val context = LocalContext.current
    val appVersion = remember(context) { context.getAppVersionName() }

    val audioFileLauncher = rememberLauncherForActivityResult(
        contract = OpenMultipleAudioDocuments(),
    ) { uris ->
        viewModel.sendIntent(SettingIntent.OnAudioFilesSelected(uris.map { it.toString() }))
    }

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
                SettingEvent.NavigateToDeviceSaveScreen -> navigateToDeviceSave()
                SettingEvent.LaunchAudioFilePicker -> audioFileLauncher.launch(arrayOf("audio/*"))
                SettingEvent.StopStepForegroundService -> StepForegroundService.stopService(context)
                is SettingEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                SettingEvent.RequestActivityRecognitionPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val permission = Manifest.permission.ACTIVITY_RECOGNITION
                        val alreadyGranted = ContextCompat.checkSelfPermission(context, permission) ==
                            PackageManager.PERMISSION_GRANTED
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
        appVersion = appVersion,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

private fun Context.getAppVersionName(): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
    return packageInfo.versionName.orEmpty()
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
            SettingSectionView(title = "다운로드") {
                SettingItemView(
                    title = "다운로드 시 맨 위에 추가",
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
                    description = uiState.playlistIdOnDownloadName,
                    onClick = { sendIntent(SettingIntent.OnClickPlaylistIdOnDownloadTile) },
                )
            }

            SettingSectionView(title = "가져오기 및 내보내기") {
                SettingItemView(
                    title = "음원 파일 선택해서 가져오기",
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
                    onClick = { sendIntent(SettingIntent.OnClickShareTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "Quick Share 화면으로 이동") },
                )
                SettingItemView(
                    title = "PC에서 공유받기",
                    onClick = { sendIntent(SettingIntent.OnClickFileShareReceiveTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "PC에서 공유받기 화면으로 이동") },
                )
                SettingItemView(
                    title = "파일 기기에 저장하기",
                    onClick = { sendIntent(SettingIntent.OnClickDeviceSaveTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "기기에 저장하기 화면으로 이동") },
                )
            }

            SettingSectionView(title = "걸음수") {
                SettingItemView(
                    title = "걸음수 이용",
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
                    onClick = { sendIntent(SettingIntent.OnClickStepTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "걸음수 현황 화면으로 이동") },
                )
            }

            SettingSectionView(title = "문제 해결") {
                SettingItemView(
                    title = "이상 데이터 점검 및 제거",
                    description = "음원 추가 오류 시 권장",
                    onClick = { sendIntent(SettingIntent.OnClickAnomalyDataCheckTile) },
                )
                SettingItemView(
                    title = "앱 로그 보기",
                    onClick = { sendIntent(SettingIntent.OnClickAppLogTile) },
                    trailing = { SettingNavigateIcon(contentDescription = "앱 로그 화면으로 이동") },
                )
            }

            SettingSectionView(title = "앱 정보") {
                SettingItemView(
                    title = "버전",
                    description = appVersion,
                    onClick = null,
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
            appVersion = "1.2.2",
            navigateToBack = {},
            sendIntent = {},
        )
    }
}

