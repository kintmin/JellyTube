package com.kintmin.presentation.platform

/** PC(데스크톱)에서 파일을 받는 기능. */
expect val isFileShareReceiveSupported: Boolean

/** 음원을 기기 공용 저장소(벨소리/알람/알림음)에 내보내는 기능. */
expect val isDeviceSaveSupported: Boolean

/** 다른 기기가 이 기기의 데이터를 받아 가게 하는 기능(보내는 쪽). */
expect val isDataMigrationSendSupported: Boolean

/** 다른 기기의 데이터를 받아 오는 기능(받는 쪽). */
expect val isDataMigrationReceiveSupported: Boolean
