package com.kintmin.presentation.platform

/** 앱이 잠시 내려가도 OS 가 허락하는 만큼 [block] 을 이어서 돌린다. 화면 꺼짐은 [KeepScreenOnEffect] 가 맡는다. */
expect suspend fun <T> continueInBackgroundWhile(block: suspend () -> T): T

/** 앱을 나가도 PC 수신 서버가 계속 받는가. */
expect val keepsReceivingInBackground: Boolean
