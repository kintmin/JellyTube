package com.kintmin.presentation.ui.setting.device_save.ringtone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.ui.setting.device_save.ActionGuideCard

private const val GARAGEBAND_RINGTONE_GUIDE_URL = "https://support.apple.com/en-us/120692"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDeviceSaveRingtoneScreen(
    navigateToBack: () -> Unit,
    onClickGoToSelect: () -> Unit,
    isOsSupported: Boolean,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        text = "벨소리·알람음으로 쓰기",
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
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
            )
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
            if (isOsSupported) {
                RingtoneGuideView(onClickGoToSelect = onClickGoToSelect)
            } else {
                RingtoneNeedsNewerOsView()
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RingtoneGuideView(onClickGoToSelect: () -> Unit) {
    Text(
        text = "JellyTube 음원을 벨소리로 등록하면\n시계 앱 알람음으로도 쓸 수 있습니다",
        style = MaterialTheme.typography.titleMedium,
    )

    ActionGuideCard(
        icon = Icons.Rounded.NotificationsActive,
        title = "벨소리로 등록할 음원 고르러 가기",
        descriptionList = listOf(
            "30초 미만의 음원만 등록할 수 있습니다",
            "더 긴 음원은 음원 편집으로 줄일 수 있습니다",
            "공유 화면 아래쪽 '더 보기'를 눌러 '벨소리로 사용'을 선택해주세요",
        ),
        onClick = onClickGoToSelect,
    )

    InfoCard(
        text = "등록하면 기기의 기본 벨소리가 바뀝니다. 알람에만 쓰려면 " +
            "등록 직후 나오는 화면에서 원래 벨소리를 다시 선택해주세요.",
    )
    InfoCard(
        text = "알람음으로 쓰려면 시계 앱 → 알람 → 사운드의 '벨소리' 목록에서 " +
            "등록한 음원을 선택해주세요. 시계 앱은 직접 열어야 합니다.",
    )
}

@Composable
private fun RingtoneNeedsNewerOsView() {
    val uriHandler = LocalUriHandler.current

    InfoCard(
        text = "이 기능은 iOS 26 이상에서 쓸 수 있습니다. " +
            "설정 > 일반 > 소프트웨어 업데이트에서 업데이트해주세요.",
    )
    TextButton(onClick = { uriHandler.openUri(GARAGEBAND_RINGTONE_GUIDE_URL) }) {
        Text("GarageBand 로 벨소리 만드는 방법 (Apple 지원)")
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Preview
@Composable
private fun SettingDeviceSaveRingtoneScreenPreview() {
    JellyTubeTheme {
        SettingDeviceSaveRingtoneScreen(
            navigateToBack = {},
            onClickGoToSelect = {},
            isOsSupported = true,
        )
    }
}

@Preview
@Composable
private fun SettingDeviceSaveRingtoneScreenNeedsNewerOsPreview() {
    JellyTubeTheme {
        SettingDeviceSaveRingtoneScreen(
            navigateToBack = {},
            onClickGoToSelect = {},
            isOsSupported = false,
        )
    }
}
