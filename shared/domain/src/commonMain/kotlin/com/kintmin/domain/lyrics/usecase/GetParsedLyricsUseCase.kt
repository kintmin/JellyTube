package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

/** 가사 파일을 읽어 줄로 나눈다. 읽지 못하면 빈 목록이다. */
class GetParsedLyricsUseCase(
    private val getAudioMediaLyricsUseCase: GetAudioMediaLyricsUseCase,
    private val parseLyricsUseCase: ParseLyricsUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(lyricFileFullPath: String): List<LyricsLine> =
        parseLyricsUseCase(
            getAudioMediaLyricsUseCase(lyricFileFullPath)
                .onFailure { appLog.sendErrorLog(ErrorLog("GetParsedLyricsUseCase", "가사 파일 읽기 실패: $lyricFileFullPath", it)) }
                .getOrNull()
                .orEmpty(),
        )
}
