package com.kintmin.presentation.port

import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.presentation.deeplink.DeepLinkParser
import com.kintmin.presentation.navigation.NavigationIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkResultNoticeTest {

    private fun destinationOf(notice: WorkResultNotice): NavigationIntent? =
        notice.deepLink?.let { DeepLinkParser.parse(it).last() }

    @Test
    fun `다운로드가 끝나면 받은 재생목록의 그 곡으로 간다`() {
        val notice = WorkResultNotice.Download.Success(artist = "가수", name = "곡", playlistId = 3, audioMediaId = 7)

        assertEquals(
            NavigationIntent.NavigateToPlaylistContent(playlistId = 3, focusAudioMediaId = 7),
            destinationOf(notice),
        )
    }

    @Test
    fun `가사 변형이 끝나면 가사 뷰어로 간다`() {
        val notice = WorkResultNotice.LyricsVariantCreation.Success(LyricsVariant.TRANSLATION, audioMediaId = 7)

        assertEquals(NavigationIntent.NavigateToLyricsViewer(7), destinationOf(notice))
    }

    @Test
    fun `편집본을 저장하면 저장된 음원 상세로 간다`() {
        assertEquals(
            NavigationIntent.NavigateToAudioMedia(7),
            destinationOf(WorkResultNotice.AudioEditSave.OverwriteSuccess(name = "곡", audioMediaId = 7)),
        )
        assertEquals(
            NavigationIntent.NavigateToAudioMedia(8),
            destinationOf(WorkResultNotice.AudioEditSave.NewMediaSuccess(name = "곡", audioMediaId = 8)),
        )
    }

    @Test
    fun `실패 알림은 앱 로그로 간다`() {
        listOf(
            WorkResultNotice.Download.Failure,
            WorkResultNotice.LyricsVariantCreation.Failure(LyricsVariant.TRANSLITERATION),
            WorkResultNotice.AnomalyCheck.Failure,
            WorkResultNotice.AudioEditSave.Failure,
        ).forEach { notice ->
            assertEquals(NavigationIntent.NavigateToSettingAppLog, destinationOf(notice))
        }
    }

    @Test
    fun `이상 데이터 점검이 끝나면 화면을 옮기지 않는다`() {
        assertNull(destinationOf(WorkResultNotice.AnomalyCheck.Success(anomalyCount = 2)))
    }
}
