package com.kintmin.presentation.ui.setting.secret_playlist_guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MobileScreenShare
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.setting.section.SettingNoticeCardView

@Composable
fun SettingSecretPlaylistGuideScreen(
    navigateToBack: () -> Unit,
    navigateToTryEntry: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = "비밀 플레이리스트",
                onClickBack = navigateToBack,
            )
        },
        bottomBar = {
            BottomActionBar {
                BottomActionButton(
                    text = "플레이리스트 화면에서 해 보기",
                    onClick = navigateToTryEntry,
                )
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "평소에는 보이지 않고,\n비밀모드를 켠 동안에만 보이는 플레이리스트입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            SettingNoticeCardView(
                icon = Icons.Rounded.TouchApp,
                title = "여는 방법",
                text = "플레이리스트 화면 맨 위의 '플레이리스트' 제목을 길게 누르면 비밀 플레이리스트 화면이 열립니다.",
            )
            SettingNoticeCardView(
                icon = Icons.Rounded.Timer,
                title = "켜 두는 시간",
                text = "비밀모드를 켤 때 정한 시간이 지나면 저절로 꺼집니다. 꺼지면 비밀 플레이리스트의 재생이 멈추고 " +
                    "비밀 플레이리스트 화면도 닫힙니다.",
            )
            SettingNoticeCardView(
                icon = Icons.Rounded.VisibilityOff,
                title = "숨겨지는 곳",
                text = "비밀 플레이리스트는 홈 목록에 나오지 않고, 그 안의 곡은 '전체'·'미분류'에서 빠집니다. " +
                    "비밀모드를 켜도 '전체'에는 나오지 않습니다 — 곡은 비밀 플레이리스트 안에서 봅니다. " +
                    "다운로드 시 재생목록으로는 비밀모드가 켜져 있을 때만 고를 수 있고, 꺼지면 미분류로 돌아갑니다.",
            )
            SettingNoticeCardView(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                title = "담으면 일어나는 일",
                text = "비밀 플레이리스트에 담은 곡은 다른 플레이리스트에서 모두 빠지고 찜도 풀립니다. 비밀에서 " +
                    "다시 빼도 원래 플레이리스트로 돌아가지 않고 미분류로 갑니다. 비밀 곡을 찜하면 일반 " +
                    "즐겨찾기가 아니라 비밀 플레이리스트 화면의 '찜한 비밀 곡'에 담깁니다.",
            )
            SettingNoticeCardView(
                icon = Icons.Rounded.MobileScreenShare,
                title = "다른 기기로 보낼 때",
                text = "비밀 플레이리스트는 비밀모드를 켠 동안에만 함께 갑니다. 꺼져 있으면 보내는 것에서 빠지므로, " +
                    "기기를 옮기는 중이라면 켜고 보내세요. 보내는 동안에는 켜 둔 시간이 멈추고, 받은 기기에서도 " +
                    "비밀모드를 켜야 보입니다.",
            )
            SettingNoticeCardView(
                icon = Icons.Rounded.Info,
                title = "알아 두세요",
                text = "화면에서 숨길 뿐 파일을 암호화하지는 않습니다. 듣는 동안에는 잠금화면과 알림의 재생 " +
                    "컨트롤에 곡 제목이 나오고, 백업 파일에도 그대로 들어갑니다.",
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview
@Composable
fun SettingSecretPlaylistGuideScreenPreview() {
    JellyTubeTheme {
        SettingSecretPlaylistGuideScreen(navigateToBack = {}, navigateToTryEntry = {})
    }
}
