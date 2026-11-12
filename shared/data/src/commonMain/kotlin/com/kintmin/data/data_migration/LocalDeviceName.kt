package com.kintmin.data.data_migration

/** 페어링할 때 보내는 기기 화면에 보여 줄 이 기기의 이름. */
internal expect suspend fun localDeviceName(): String
