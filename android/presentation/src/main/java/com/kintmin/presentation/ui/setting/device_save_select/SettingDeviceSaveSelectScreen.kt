package com.kintmin.presentation.ui.setting.device_save_select

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.device_save_select.list.SettingDeviceSaveSelectListItemView
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDeviceSaveSelectScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<SettingDeviceSaveSelectViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.sendIntent(SettingDeviceSaveSelectIntent.OnClickSaveToDevice)
        } else {
            Toast.makeText(context, "저장 권한이 없어 기기에 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingDeviceSaveSelectEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SettingDeviceSaveSelectScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        onClickSave = {
            val needsLegacyPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED

            if (needsLegacyPermission) {
                legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                viewModel.sendIntent(SettingDeviceSaveSelectIntent.OnClickSaveToDevice)
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "기기에 저장할 음원 선택",
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                actions = {
                    if (uiState.checkedItemCount > 0) {
                        TextButton(
                            onClick = {
                                sendIntent(SettingDeviceSaveSelectIntent.OnClickClearCheckedItemList)
                            },
                        ) {
                            Text(
                                text = "${uiState.checkedItemCount} 선택 해제",
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                            )
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
                ExportTargetSelector(
                    selectedTargetList = uiState.selectedTargetList,
                    sendIntent = sendIntent,
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RectangleShape,
                    enabled = uiState.isSaveEnabled,
                    onClick = onClickSave,
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text = "${uiState.checkedItemCount}개 기기에 저장")
                }
                Box(
                    modifier = Modifier
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
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
                    .clip(RoundedCornerShape(4))
                    .padding(horizontal = 16.dp),
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp, end = 4.dp),
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                )
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .padding(vertical = 16.dp),
                    maxLines = 1,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    value = searchText,
                    onValueChange = { newText ->
                        searchText = newText
                        sendIntent(SettingDeviceSaveSelectIntent.OnChangeSearchText(newText))
                    },
                )
            }
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
                        Text(
                            text = target.displayName,
                            fontSize = 12.sp,
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
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

@Preview(showBackground = true)
@Composable
private fun ExportTargetSelectorPreview() {
    JellyTubeTheme {
        ExportTargetSelector(
            selectedTargetList = SettingDeviceSaveSelectUiState.DEFAULT_TARGET_LIST,
            sendIntent = {},
        )
    }
}

@Preview(showBackground = true)
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
