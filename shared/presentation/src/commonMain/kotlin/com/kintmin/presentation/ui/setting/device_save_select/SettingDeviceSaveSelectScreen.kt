package com.kintmin.presentation.ui.setting.device_save_select

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.presentation.platform.AppPermission
import com.kintmin.presentation.platform.DeviceSaveFlow
import com.kintmin.presentation.platform.deviceSaveFlow
import com.kintmin.presentation.platform.rememberAudioFileSharer
import com.kintmin.presentation.platform.rememberPermissionController
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.SearchField
import com.kintmin.presentation.ui.setting.device_save_select.dialog.RingtoneDefaultChangeDialog
import com.kintmin.presentation.ui.setting.device_save_select.dialog.RingtoneTooLongDialog
import com.kintmin.presentation.ui.setting.device_save_select.list.SettingDeviceSaveSelectListItemView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingDeviceSaveSelectScreen(
    navigateToBack: () -> Unit,
    navigateToAudioEdit: (audioMediaId: Int) -> Unit,
) {
    val viewModel = koinViewModel<SettingDeviceSaveSelectViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val toaster = rememberToaster()
    val permissionController = rememberPermissionController()
    val audioFileSharer = rememberAudioFileSharer()
    var tooLongAudioMediaId by remember { mutableStateOf<Int?>(null) }
    var pendingRingtoneShare by remember {
        mutableStateOf<SettingDeviceSaveSelectEvent.ConfirmRingtoneShare?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingDeviceSaveSelectEvent.ShowToast ->
                    toaster.show(event.message)

                is SettingDeviceSaveSelectEvent.ShowRingtoneTooLong ->
                    tooLongAudioMediaId = event.audioMediaId

                is SettingDeviceSaveSelectEvent.ConfirmRingtoneShare ->
                    pendingRingtoneShare = event
            }
        }
    }

    RingtoneTooLongDialog(
        isShow = tooLongAudioMediaId != null,
        onDismiss = { tooLongAudioMediaId = null },
        onConfirmEdit = { tooLongAudioMediaId?.let(navigateToAudioEdit) },
    )
    RingtoneDefaultChangeDialog(
        isShow = pendingRingtoneShare != null,
        onDismiss = { pendingRingtoneShare = null },
        onConfirmShare = {
            pendingRingtoneShare?.let { share ->
                if (!audioFileSharer.share(share.fileFullPath, share.title, share.artist)) {
                    toaster.show("공유 화면을 열 수 없습니다.")
                }
            }
        },
    )

    SettingDeviceSaveSelectScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        onClickSave = {
            // 저장 권한이 OS 버전별로 필요한지는 구현이 판단한다.
            if (permissionController.isGranted(AppPermission.MediaStorage)) {
                viewModel.sendIntent(SettingDeviceSaveSelectIntent.OnClickSaveToDevice)
            } else {
                permissionController.request(listOf(AppPermission.MediaStorage)) { result ->
                    if (result[AppPermission.MediaStorage] == true) {
                        viewModel.sendIntent(SettingDeviceSaveSelectIntent.OnClickSaveToDevice)
                    } else {
                        toaster.show("저장 권한이 없어 기기에 저장할 수 없습니다.")
                    }
                }
            }
        },
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDeviceSaveSelectScreen(
    uiState: SettingDeviceSaveSelectUiState,
    navigateToBack: () -> Unit,
    onClickSave: () -> Unit,
    sendIntent: (SettingDeviceSaveSelectIntent) -> Unit,
) {
    var searchText by remember { mutableStateOf("") }
    val isRingtoneShareFlow = deviceSaveFlow == DeviceSaveFlow.RingtoneShareSheet

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = if (isRingtoneShareFlow) "벨소리로 쓸 음원 선택" else "기기에 저장할 음원 선택",
                onClickBack = navigateToBack,
                actions = {
                    if (uiState.checkedItemCount > 0) {
                        TextButton(
                            colors = JellyTubeButtonColors.text(),
                            onClick = {
                                sendIntent(SettingDeviceSaveSelectIntent.OnClickClearCheckedItemList)
                            },
                        ) {
                            Text(text = "${uiState.checkedItemCount}개 선택 해제")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .imePadding()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                if (!isRingtoneShareFlow) {
                    ExportTargetSelector(
                        selectedTargetList = uiState.selectedTargetList,
                        sendIntent = sendIntent,
                    )
                }
                BottomActionBar {
                    BottomActionButton(
                        text = when {
                            uiState.isSaving -> "저장 중…"
                            isRingtoneShareFlow -> "벨소리로 사용"
                            else -> "${uiState.checkedItemCount}개 기기에 저장"
                        },
                        enabled = uiState.isSaveEnabled,
                        onClick = onClickSave,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                value = searchText,
                placeholder = "음원 제목으로 검색",
                onValueChange = { newText ->
                    searchText = newText
                    sendIntent(SettingDeviceSaveSelectIntent.OnChangeSearchText(newText))
                },
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = uiState.audioMediaList.size,
                    key = { index -> uiState.audioMediaList[index].id },
                ) { index ->
                    SettingDeviceSaveSelectListItemView(
                        modifier = Modifier.height(56.dp),
                        data = uiState.audioMediaList[index],
                        sendIntent = sendIntent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportTargetSelector(
    selectedTargetList: List<AudioExportTarget>,
    sendIntent: (SettingDeviceSaveSelectIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "어떤 용도로 쓸지 고르세요",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "고른 용도마다 복사본이 하나씩 생깁니다",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AudioExportTarget.entries.forEach { target ->
                val isSelected = selectedTargetList.contains(target)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        sendIntent(SettingDeviceSaveSelectIntent.OnToggleExportTarget(target))
                    },
                    label = {
                        Text(text = target.displayName)
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExportTargetSelectorPreview() {
    JellyTubeTheme {
        ExportTargetSelector(
            selectedTargetList = SettingDeviceSaveSelectUiState.DEFAULT_TARGET_LIST,
            sendIntent = {},
        )
    }
}

@Preview
@Composable
private fun SettingDeviceSaveSelectScreenPreview() {
    JellyTubeTheme {
        SettingDeviceSaveSelectScreen(
            uiState = SettingDeviceSaveSelectUiState.getMock(),
            navigateToBack = {},
            onClickSave = {},
            sendIntent = {},
        )
    }
}
