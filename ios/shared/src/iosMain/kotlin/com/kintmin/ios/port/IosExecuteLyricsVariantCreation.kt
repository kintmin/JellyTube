package com.kintmin.ios.port

import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.CreateLyricsVariantUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.port.ExecuteLyricsVariantCreation
import com.kintmin.presentation.port.WorkResultNotice
import com.kintmin.presentation.port.model.RunningWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 가사 번역·음차 변형 생성. [IosExecuteAnomalyDataCheck] 와 같은 성격이다 — 계산 작업이라
 * 전송 경로와 무관하고, 앱 수명 스코프에서 돌고 끝나면 로컬 알림으로 알린다.
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
            IosRunningWorkTracker.track(RunningWork.LYRICS_VARIANT) {
                createLyricsVariantUseCase(lyricFileFullPath, variant, sourceLanguage)
            }
                .onSuccess {
                    IosLocalNotification.show(WorkResultNotice.LyricsVariantCreation.Success(variant, audioMediaId))
                }
                .onFailure { throwable ->
                    appLog.sendDebugLog(
                        DebugLog("IosExecuteLyricsVariantCreation", throwable.message.toString()),
                    )
                    IosLocalNotification.show(WorkResultNotice.LyricsVariantCreation.Failure(variant))
                }
        }
    }
}
