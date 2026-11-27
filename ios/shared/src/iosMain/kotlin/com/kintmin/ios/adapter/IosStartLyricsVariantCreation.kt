package com.kintmin.ios.adapter

import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.CreateLyricsVariantUseCase
import com.kintmin.ios.notification.IosLocalNotification
import com.kintmin.ios.task.IosRunningTaskTracker
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.presentation.port.StartLyricsVariantCreation
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.port.model.WorkResultNotice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 계산 작업이라 전송 경로와 무관하다. 앱 수명 스코프에서 돌고 끝나면 로컬 알림으로 알린다.
 */
internal class IosStartLyricsVariantCreation(
    private val createLyricsVariantUseCase: CreateLyricsVariantUseCase,
    private val appLog: AppLog,
) : StartLyricsVariantCreation {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun invoke(
        audioMediaId: Int,
        lyricFileFullPath: String,
        variant: LyricsVariant,
    ) {
        scope.launch {
            IosRunningTaskTracker.track(RunningTask.LYRICS_VARIANT) {
                createLyricsVariantUseCase(audioMediaId, lyricFileFullPath, variant)
            }
                .onSuccess {
                    IosLocalNotification.show(WorkResultNotice.LyricsVariantCreation.Success(variant, audioMediaId))
                }
                .onFailure { throwable ->
                    appLog.sendErrorLog(ErrorLog("IosStartLyricsVariantCreation", "가사 변형 만들기 실패", throwable))
                    IosLocalNotification.show(WorkResultNotice.LyricsVariantCreation.Failure(variant))
                }
        }
    }
}
