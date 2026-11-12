package com.kintmin.presentation.port

import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.presentation.deeplink.DeepLinkConstants

/** 작업 결과 알림의 문구와 탭했을 때 갈 화면. 두 플랫폼이 같은 알림을 띄워야 해서 여기 한 벌만 둔다. */
sealed interface WorkResultNotice {

    val title: String
    val body: String
    val deepLink: String?

    sealed interface Download : WorkResultNotice {

        data class Success(
            val artist: String,
            val name: String,
            val playlistId: Int,
            val audioMediaId: Int,
        ) : Download {
            override val title = "음원 다운로드 완료"
            override val body = "$artist - $name"
            override val deepLink = DeepLinkConstants.UrlBuilder.playlistContentScreen(playlistId, audioMediaId)
        }

        data object Failure : Download {
            override val title = "음원 다운로드 실패"
            override val body = "다운에 실패했습니다. 로그를 확인해주세요."
            override val deepLink = DeepLinkConstants.UrlBuilder.appLogScreen()
        }
    }

    sealed interface LyricsVariantCreation : WorkResultNotice {

        data class Success(
            val variant: LyricsVariant,
            val audioMediaId: Int,
        ) : LyricsVariantCreation {
            override val title = "${variant.label} 완료"
            override val body = "탭하여 결과를 확인하세요."
            override val deepLink = DeepLinkConstants.UrlBuilder.lyricsViewerScreen(audioMediaId)
        }

        data class Failure(
            val variant: LyricsVariant,
        ) : LyricsVariantCreation {
            override val title = "${variant.label} 실패"
            override val body = "번역에 실패했습니다. 로그를 확인해주세요."
            override val deepLink = DeepLinkConstants.UrlBuilder.appLogScreen()
        }
    }

    sealed interface AnomalyCheck : WorkResultNotice {

        data class Success(
            val anomalyCount: Int,
        ) : AnomalyCheck {
            override val title = "이상 데이터 점검 완료"
            override val body = "이상 데이터 점검에 완료했습니다.\n발견된 이상 데이터: ${anomalyCount}건"
            override val deepLink: String? = null
        }

        data object Failure : AnomalyCheck {
            override val title = "이상 데이터 점검 실패"
            override val body = "작업 도중 오류가 발생했습니다\n앱 로그를 확인해주세요."
            override val deepLink = DeepLinkConstants.UrlBuilder.appLogScreen()
        }
    }
}
