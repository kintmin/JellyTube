package com.kintmin.presentation.platform

/**
 * 플랫폼이 제공하지 않는 기능의 진입점을 숨기는 데 쓴다. 포트를 미지원 스텁으로 막는 것과는
 * 별개다 — 막아도 진입점이 보이면 눌리고, 그러면 아무 일도 하지 않는 화면이 뜬다.
 *
 * **기능 플래그가 아니다.** 플랫폼이 그 기능을 아예 제공하지 않을 때만 false 다.
 * 켜고 끄는 설정은 DataStore 에 둔다.
 */
expect val isAudioEditSupported: Boolean

/** PC(데스크톱)에서 파일을 받는 기능. */
expect val isFileShareReceiveSupported: Boolean

/** 음원을 기기 공용 저장소(벨소리/알람/알림음)에 내보내는 기능. */
expect val isDeviceSaveSupported: Boolean

/** 다른 기기가 이 기기의 데이터를 받아 가게 하는 기능(보내는 쪽). */
expect val isDataMigrationSendSupported: Boolean

/** 다른 기기의 데이터를 받아 오는 기능(받는 쪽). */
expect val isDataMigrationReceiveSupported: Boolean
