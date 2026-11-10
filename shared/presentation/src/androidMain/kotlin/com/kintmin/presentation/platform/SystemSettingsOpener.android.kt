package com.kintmin.presentation.platform

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * 목적지별 인텐트 후보. 앞에서부터 시도해 하나라도 뜨면 성공이다.
 *
 * SHOW_ALARMS 를 처리하지 않는 기기가 있어 알람 생성 화면(SET_ALARM)으로 폴백한다.
 * SET_ALARM 은 EXTRA_SKIP_UI 를 false 로 둬야 알람을 바로 만들지 않고 화면을 보여준다.
 */
private fun SystemSettingsTarget.intentCandidates(packageName: String): List<Intent> = when (this) {
    SystemSettingsTarget.Alarm -> listOf(
        Intent(AlarmClock.ACTION_SHOW_ALARMS),
        Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_SKIP_UI, false),
    )

    SystemSettingsTarget.Sound -> listOf(
        Intent(Settings.ACTION_SOUND_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )

    SystemSettingsTarget.AppPermissions -> listOf(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()),
        Intent(Settings.ACTION_SETTINGS),
    )
}

/**
 * 실패 원인이 액티비티 부재(ActivityNotFoundException)인지 권한 거부(SecurityException)인지
 * 기기마다 달라서 종류를 가리지 않고 다음 후보로 넘어간다.
 */
private fun Context.startFirstAvailable(intentList: List<Intent>): Boolean {
    intentList.forEach { intent ->
        val isStarted = runCatching {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (isStarted) return true
    }
    return false
}

@Composable
actual fun rememberSystemSettingsOpener(): SystemSettingsOpener {
    val context = LocalContext.current
    return remember(context) {
        SystemSettingsOpener { target ->
            context.startFirstAvailable(target.intentCandidates(context.packageName))
        }
    }
}
