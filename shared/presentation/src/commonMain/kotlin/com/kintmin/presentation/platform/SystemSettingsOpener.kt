package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

enum class SystemSettingsTarget {

    /** 알람 목록·알람 생성 화면. */
    Alarm,

    /** 소리·볼륨 설정 화면. */
    Sound,

    /** 이 앱의 설정 페이지. 거부된 권한을 사용자가 직접 켜는 자리다. */
    AppPermissions,
}

/** 열지 못하면 false. 보여 줄 문구는 호출자가 정한다. */
fun interface SystemSettingsOpener {

    fun open(target: SystemSettingsTarget): Boolean
}

@Composable
expect fun rememberSystemSettingsOpener(): SystemSettingsOpener
