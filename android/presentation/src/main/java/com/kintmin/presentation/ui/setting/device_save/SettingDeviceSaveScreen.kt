package com.kintmin.presentation.ui.setting.device_save

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessAlarm
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SaveAlt
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun SettingDeviceSaveScreen(
    navigateToBack: () -> Unit,
    navigateToDeviceSaveSelect: () -> Unit,
) {
    val context = LocalContext.current

    SettingDeviceSaveScreen(
        navigateToBack = navigateToBack,
        onClickGoToSave = navigateToDeviceSaveSelect,
        onClickGoToAlarm = {
            // SHOW_ALARMS를 처리하지 않는 기기가 있어서 알람 생성 화면(SET_ALARM)으로 폴백한다.
            // SET_ALARM은 EXTRA_SKIP_UI를 false로 둬야 알람을 바로 만들지 않고 화면을 보여준다.
            startFirstAvailableActivity(
                context = context,
                intentList = listOf(
                    Intent(AlarmClock.ACTION_SHOW_ALARMS),
                    Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, false),
                ),
                notFoundMessage = "시계 앱을 열 수 없습니다. 시계 앱에서 직접 알람음을 설정해주세요.",
            )
        },
        onClickGoToSoundSetting = {
            startFirstAvailableActivity(
                context = context,
                intentList = listOf(
                    Intent(Settings.ACTION_SOUND_SETTINGS),
                    Intent(Settings.ACTION_SETTINGS),
                ),
                notFoundMessage = "소리 설정 화면을 열 수 없습니다.",
            )
        },
    )
}

/**
 * 앞에서부터 실행 가능한 인텐트를 찾아 하나만 실행한다.
 *
 * 기기·제조사에 따라 처리하는 액션이 달라서 폴백이 필요하다. 패키지 가시성(Android 11+) 때문에
 * resolveActivity로 미리 확인하는 방식은 믿을 수 없으므로 실제로 실행해 보고 판단한다.
 *
 * 실패 원인이 액티비티 부재(ActivityNotFoundException)인지 권한 거부(SecurityException)인지
 * 기기마다 달라서 종류를 가리지 않고 다음 후보로 넘어간다. 전부 실패했을 때만 안내한다.
 */
private fun startFirstAvailableActivity(
    context: Context,
    intentList: List<Intent>,
    notFoundMessage: String,
) {
    intentList.forEach { intent ->
        val isStarted = runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (isStarted) return
    }
    Toast.makeText(context, notFoundMessage, Toast.LENGTH_LONG).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDeviceSaveScreen(
    navigateToBack: () -> Unit,
    onClickGoToSave: () -> Unit,
    onClickGoToAlarm: () -> Unit,
    onClickGoToSoundSetting: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("파일 기기에 저장하기") },
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
            Text(
                text = "JellyTube 음원을 기기에 저장하면\n다른 앱과 알람음으로 쓸 수 있습니다",
                style = MaterialTheme.typography.titleMedium,
            )

            ActionGuideCard(
                icon = Icons.Rounded.SaveAlt,
                title = "기기에 파일 저장하러 가기",
                descriptionList = listOf(
                    "음악 앱과 파일 관리자에서 바로 볼 수 있습니다",
                    "다른 앱에서도 저장한 음원을 쓸 수 있습니다",
                    "저장할 때 알람음·알림음·벨소리 중 쓸 용도를 고를 수 있습니다",
                ),
                onClick = onClickGoToSave,
            )

            ActionGuideCard(
                icon = Icons.Rounded.AccessAlarm,
                title = "알람음으로 설정하러 가기",
                descriptionList = listOf(
                    "원하는 알람을 고르고 알람음 설정으로 이동해주세요",
                    "알람음 목록에서 기기에 저장한 JellyTube 음원을 선택해주세요",
                    "짧은 음원은 다시 울림을 계속 반복으로 설정해주세요",
                ),
                onClick = onClickGoToAlarm,
            )

            ActionGuideCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "알림음·벨소리 설정하러 가기",
                descriptionList = listOf(
                    "소리 설정에서 전화 벨소리와 기본 알림음을 바꿀 수 있습니다",
                    "목록에서 기기에 저장한 JellyTube 음원을 선택해주세요",
                ),
                onClick = onClickGoToSoundSetting,
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "기기에 저장하기 전에는 알람음·알림음·벨소리 목록에 나타나지 않습니다. " +
                        "먼저 음원을 저장한 뒤 설정 화면으로 이동해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionGuideCard(
    icon: ImageVector,
    title: String,
    descriptionList: List<String>,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            descriptionList.forEach { description ->
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "· ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionGuideCardPreview() {
    JellyTubeTheme {
        ActionGuideCard(
            icon = Icons.Rounded.SaveAlt,
            title = "기기에 파일 저장하러 가기",
            descriptionList = listOf(
                "음악 앱과 파일 관리자에서 바로 볼 수 있습니다",
                "다른 앱에서도 저장한 음원을 쓸 수 있습니다",
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingDeviceSaveScreenPreview() {
    JellyTubeTheme {
        SettingDeviceSaveScreen(
            navigateToBack = {},
            onClickGoToSave = {},
            onClickGoToAlarm = {},
            onClickGoToSoundSetting = {},
        )
    }
}
