package com.kintmin.presentation.ui.setting.file_share_receive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.platform.keepsReceivingInBackground
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.setting.section.SettingNoticeCardView
import com.kintmin.presentation.ui.setting.section.SettingStepCardView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingFileShareReceiveScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<SettingFileShareReceiveViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SettingFileShareReceiveScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingFileShareReceiveScreen(
    uiState: SettingFileShareReceiveUiState,
    navigateToBack: () -> Unit,
    sendIntent: (SettingFileShareReceiveIntent) -> Unit,
) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = "PC에서 공유받기",
                onClickBack = navigateToBack,
            )
        },
        bottomBar = {
            BottomActionBar {
                when (uiState.serverStatus) {
                    ServerStatus.STOPPED -> BottomActionButton(
                        text = "파일 공유 받기 시작",
                        onClick = { sendIntent(SettingFileShareReceiveIntent.OnClickStartServer) },
                    )
                    ServerStatus.RUNNING -> BottomActionButton(
                        text = "파일 공유 받기 중지",
                        onClick = { sendIntent(SettingFileShareReceiveIntent.OnClickStopServer) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ServerStatusLabel(serverStatus = uiState.serverStatus)
                    // 라벨 너비가 제각각이라 간격을 나누면 가운데 노드가 중앙에서 밀린다.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        FlowNode(
                            icon = Icons.Rounded.Computer,
                            label = "PC JellyTube",
                            modifier = Modifier.weight(1f),
                        )
                        FlowArrow()
                        FlowNode(
                            icon = Icons.Rounded.Wifi,
                            label = "Wi-Fi",
                            modifier = Modifier.weight(1f),
                        )
                        FlowArrow()
                        FlowNode(
                            icon = Icons.Rounded.MusicNote,
                            label = "JellyTube",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = when (uiState.serverStatus) {
                            ServerStatus.STOPPED -> "PC의 JellyTube에서 이 기기로 음원을 보냅니다.\n아래 버튼으로 받기를 시작해 주세요."
                            ServerStatus.RUNNING -> "받을 준비가 됐습니다.\nPC에서 파일을 보내 주세요."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            SettingNoticeCardView(
                icon = Icons.Rounded.Wifi,
                title = "같은 Wi-Fi 네트워크 필요",
                text = "PC와 이 기기가 같은 Wi-Fi에 연결되어 있어야 합니다.",
            )

            if (!keepsReceivingInBackground) {
                SettingNoticeCardView(
                    icon = Icons.Rounded.Warning,
                    title = "앱을 나가면 연결이 끊길 수 있음",
                    text = "다른 앱으로 전환하거나 기기를 잠그면 파일을 받지 못합니다. 받는 동안에는 JellyTube를 켜 두세요. 공유를 받는 동안에는 화면이 저절로 꺼지지 않습니다.",
                )
            }

            SettingStepCardView(
                stepNumber = 1,
                title = "파일 공유 받기 시작",
                description = "아래 버튼을 눌러 파일 수신 서버를 시작합니다. 서버가 켜져 있는 동안 PC에서 파일을 전송할 수 있습니다.",
            )
            SettingStepCardView(
                stepNumber = 2,
                title = "PC에서 JellyTube 앱 실행",
                description = "PC에서 JellyTube 파일 공유 앱을 실행합니다. 이 기기를 자동으로 검색합니다.",
            )
            SettingStepCardView(
                stepNumber = 3,
                title = "파일 드래그 앤 드롭",
                description = "PC 앱에 음원 파일을 끌어다 놓으면 자동으로 이 기기의 라이브러리에 추가됩니다.",
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * 받는 중은 화면을 나가도 이어지는 모드다 — 색만으로 알리면 끊긴 줄 모른 채 나간다. 그래서
 * 글자와 채움·테두리로 같이 말한다.
 */
@Composable
private fun ServerStatusLabel(serverStatus: ServerStatus) {
    val isRunning = serverStatus == ServerStatus.RUNNING
    Surface(
        color = if (isRunning) MaterialTheme.colorScheme.tertiary else Color.Transparent,
        contentColor = if (isRunning) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        shape = CircleShape,
        border = if (isRunning) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = if (isRunning) "받는 중" else "꺼져 있음",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private val FlowNodeIconSize = 28.dp

/** 화살표는 라벨이 없다 — 아이콘과 같은 높이 안에서 가운데로 둬야 아이콘 줄에 선다. */
@Composable
private fun FlowArrow() {
    Box(
        modifier = Modifier.height(FlowNodeIconSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun FlowNode(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(FlowNodeIconSize),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview
@Composable
private fun SettingFileShareReceiveScreenIdlePreview() {
    JellyTubeTheme {
        SettingFileShareReceiveScreen(
            uiState = SettingFileShareReceiveUiState(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}

@Preview
@Composable
private fun SettingFileShareReceiveScreenRunningPreview() {
    JellyTubeTheme {
        SettingFileShareReceiveScreen(
            uiState = SettingFileShareReceiveUiState(serverStatus = ServerStatus.RUNNING),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}

