package com.kintmin.presentation.ui.audio_media_detail

import com.kintmin.domain.audio_edit.model.EditedAudioSource
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_track.model.AudioTrack
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AudioMediaDetailUiStateTest {

    @Test
    fun `조회 결과가 비면 삭제된 음원 상태가 된다`() {
        val uiState = emptyList<PlaylistTrackAggregate>().toAudioMediaDetailUiState()

        assertTrue(uiState.isDeleted)
        assertEquals(-1, uiState.audioMediaId)
        assertTrue(uiState.playlists.isEmpty())
    }

    @Test
    fun `음원이 있으면 삭제된 음원 상태가 아니다`() {
        val uiState = listOf(aggregate()).toAudioMediaDetailUiState()

        assertFalse(uiState.isDeleted)
        assertEquals(7, uiState.audioMediaId)
    }

    @Test
    fun `생성 시각은 초 없이 한국어로 적는다`() {
        val uiState = listOf(aggregate()).toAudioMediaDetailUiState()

        assertEquals("2026년 11월 20일 14시 30분", uiState.audioMediaCreationTime)
    }

    @Test
    fun `웹에서 받은 음원은 출처를 열 수 있다`() {
        val uiState = listOf(aggregate(source = "https://youtube.com/watch?v=abc"))
            .toAudioMediaDetailUiState()

        assertEquals("https://youtube.com/watch?v=abc", uiState.webViewUrl)
    }

    @Test
    fun `기기에서 가져온 음원은 출처를 열 수 없다`() {
        val uiState = listOf(aggregate(source = "fileShare://abc")).toAudioMediaDetailUiState()

        assertNull(uiState.webViewUrl)
        assertEquals("fileShare://abc", uiState.source)
    }

    @Test
    fun `편집본은 원본 주소를 출처로 꺼내 쓴다`() {
        val source = EditedAudioSource.build(uniqueId = "1", originSource = "https://youtube.com/watch?v=abc")

        val uiState = listOf(aggregate(source = source)).toAudioMediaDetailUiState()

        assertEquals("https://youtube.com/watch?v=abc", uiState.webViewUrl)
    }

    private fun aggregate(source: String = "https://youtube.com/watch?v=abc") = PlaylistTrackAggregate(
        audioMedia = AudioMedia(
            id = 7,
            source = source,
            name = "밤편지",
            artist = "아이유",
            description = "",
            audioDuration = 222.seconds,
            audioFileFullPath = "/audio/7.mp3",
            imageFileFullPath = null,
            createdTime = CREATED_TIME,
        ),
        playlist = Playlist(
            id = 3,
            name = "즐겨 듣는 곡",
            description = "",
            audioMediaCount = 1,
            playTimeDuration = 222.seconds,
            createdTime = CREATED_TIME,
            imageFileFullPath = null,
            isCustomImage = false,
            sequence = 0,
            type = PlaylistType.USER,
        ),
        audioTrack = AudioTrack(
            audioMediaId = 7,
            playlistId = 3,
            trackSequence = 0,
            trackAddedTime = CREATED_TIME,
        ),
    )

    private companion object {
        val CREATED_TIME = LocalDateTime(2026, 11, 20, 14, 30)
    }
}
