package com.kintmin.presentation.ui.setting.file_share_receive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun SettingFileShareReceiveScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = hiltViewModel<SettingFileShareReceiveViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SettingFileShareReceiveScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        onStartServer = viewModel::onStartServer,
        onStopServer = viewModel::onStopServer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingFileShareReceiveScreen(
    uiState: SettingFileShareReceiveUiState,
    navigateToBack: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("파일 공유 받기") },
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
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (uiState.serverStatus == ServerStatus.RUNNING) {
                    Text(
                        text = "서버가 실행 중입니다. PC에서 파일을 전송해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                when (uiState.serverStatus) {
                    ServerStatus.IDLE -> Button(
                        onClick = onStartServer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("파일 공유 받기 시작")
                    }
                    ServerStatus.RUNNING -> Button(
                        onClick = onStopServer,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text("파일 공유 받기 중지")
                    }
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
            Text(
                text = "Windows PC에서 음원 파일을 젤리튜브로 전송하는 방법",
                style = MaterialTheme.typography.titleMedium,
            )

            // 흐름도 다이어그램
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlowNode(icon = Icons.Rounded.Computer, label = "Windows PC")
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowNode(icon = Icons.Rounded.Wifi, label = "Wi-Fi")
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowNode(icon = Icons.Rounded.MusicNote, label = "젤리튜브")
                }
            }

            // 사전 조건 안내
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "같은 Wi-Fi 네트워크 필요",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "PC와 이 기기가 같은 Wi-Fi에 연결되어 있어야 합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            // 단계별 안내
            StepCard(
                stepNumber = 1,
                title = "파일 공유 받기 시작",
                description = "아래 버튼을 눌러 파일 수신 서버를 시작합니다. 서버가 켜져 있는 동안 PC에서 파일을 전송할 수 있습니다.",
            )
            StepCard(
                stepNumber = 2,
                title = "PC에서 JellyTube 앱 실행",
                description = "Windows PC에서 JellyTube 파일 공유 앱을 실행합니다. 이 기기를 자동으로 검색합니다.",
            )
            StepCard(
                stepNumber = 3,
                title = "파일 드래그 앤 드롭",
                description = "PC 앱에 음원 파일을 끌어다 놓으면 자동으로 이 기기의 라이브러리에 추가됩니다.",
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FlowNode(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepCard(stepNumber: Int, title: String, description: String) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$stepNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingFileShareReceiveScreenIdlePreview() {
    JellyTubeTheme {
        SettingFileShareReceiveScreen(
            uiState = SettingFileShareReceiveUiState(),
            navigateToBack = {},
            onStartServer = {},
            onStopServer = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingFileShareReceiveScreenRunningPreview() {
    JellyTubeTheme {
        SettingFileShareReceiveScreen(
            uiState = SettingFileShareReceiveUiState(serverStatus = ServerStatus.RUNNING),
            navigateToBack = {},
            onStartServer = {},
            onStopServer = {},
        )
    }
}
