package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 열려는 시스템 설정 화면. */
enum class SystemSettingsTarget {

    /** 알람 목록·알람 생성 화면. */
    Alarm,

    /** 소리·볼륨 설정 화면. */
    Sound,
}

/**
 * 시스템 설정 화면을 연다.
 *
 * 어떤 인텐트·URL 로 가는지, 기기가 그 목적지를 지원하는지는 플랫폼마다 다르다.
 * 열지 못하면 false 를 돌려주고, 사용자에게 보여줄 문구는 호출자가 정한다.
 */
fun interface SystemSettingsOpener {

    fun open(target: SystemSettingsTarget): Boolean
}

@Composable
expect fun rememberSystemSettingsOpener(): SystemSettingsOpener
