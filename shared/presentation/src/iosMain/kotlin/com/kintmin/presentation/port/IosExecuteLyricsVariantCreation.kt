package com.kintmin.presentation.port

import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.CreateLyricsVariantUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 가사 번역·음차 변형 생성.
 *
 * [IosExecuteAnomalyDataCheck] 와 같은 성격이다 — 계산 작업이라 전송 경로와 무관하고,
 * 앱 수명 스코프에서 돌고 끝나면 로컬 알림으로 알린다.
 *
 * Android 와 달리 알림을 눌러도 가사 화면으로 가지 않는다. 그러려면
 * `UNUserNotificationCenterDelegate` 가 Swift 쪽에 있어야 한다.
 */
internal class IosExecuteLyricsVariantCreation(
    private val createLyricsVariantUseCase: CreateLyricsVariantUseCase,
    private val appLog: AppLog,
) : ExecuteLyricsVariantCreation {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun invoke(
        audioMediaId: Int,
        lyricFileFullPath: String,
        variant: LyricsVariant,
        sourceLanguage: String,
    ) {
        scope.launch {
            createLyricsVariantUseCase(lyricFileFullPath, variant, sourceLanguage)
                .onSuccess {
                    IosLocalNotification.show(
                        title = "${variant.label()} 완료",
                        body = "탭하여 결과를 확인하세요.",
                    )
                }
                .onFailure { throwable ->
                    appLog.sendDebugLog(
                        DebugLog("IosExecuteLyricsVariantCreation", throwable.message.toString()),
                    )
                    IosLocalNotification.show(
                        title = "${variant.label()} 실패",
                        body = "번역에 실패했습니다. 로그를 확인해주세요.",
                    )
                }
        }
    }
}

/** 알림 문구는 Android 의 `LyricsVariantResultNotification` 과 같은 말을 쓴다. */
private fun LyricsVariant.label(): String = when (this) {
    LyricsVariant.TRANSLATION -> "가사 번역"
    LyricsVariant.TRANSLITERATION -> "가사 음차 번역"
}
