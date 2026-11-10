package com.kintmin.ios.media

import kotlin.test.Test
import kotlin.test.assertEquals

/** 구간반복의 판정은 공통 `RepeatRange` 가 하고(`RepeatRangeTest`), 여기는 배선만 본다. */
class IosMediaControllerManagerTest {

    private val bridge = FakeMediaControllerBridge()
    private val manager = IosMediaControllerManager(bridge)

    @Test
    fun `끝점을 넘으면 브릿지에 시작점으로 탐색을 건다`() {
        bridge.positionMillis = 1_000
        manager.updateRepeatRange()
        bridge.positionMillis = 3_000
        manager.updateRepeatRange()

        bridge.positionMillis = 3_001
        manager.repeatRangeIfNeeded()

        assertEquals(listOf(1_000L), bridge.seekCalls)
    }

    @Test
    fun `구간이 덜 잡혔으면 탐색하지 않는다`() {
        bridge.positionMillis = 1_000
        manager.updateRepeatRange()

        bridge.positionMillis = 9_000
        manager.repeatRangeIfNeeded()

        assertEquals(emptyList(), bridge.seekCalls)
    }

    @Test
    fun `재생 중인 곡이 없으면 아무 일도 하지 않는다`() {
        bridge.playing = null

        manager.repeatRangeIfNeeded()

        assertEquals(emptyList(), bridge.seekCalls)
        assertEquals(null, manager.repeatRangeState.mediaId)
    }

    private class FakeMediaControllerBridge : IosMediaControllerBridge {
        var positionMillis = 0L
        var playing: IosMediaItem? = IosMediaItem(
            mediaId = "1",
            fileFullPath = "/tmp/1.m4a",
            title = "title",
            description = "",
            artist = "artist",
            durationMillis = 10_000,
            artworkFileFullPath = null,
        )
        val seekCalls = mutableListOf<Long>()

        override fun playingItem() = playing
        override fun currentPositionMillis() = positionMillis
        override fun seek(positionMillis: Long) {
            seekCalls += positionMillis
        }

        override fun initialize() = Unit
        override fun play(playlistId: Int, startMediaId: Int?, items: List<IosMediaItem>) = Unit
        override fun pause() = Unit
        override fun resume() = Unit
        override fun playPrevious() = Unit
        override fun playNext() = Unit
        override fun isPlaying() = false
        override fun durationMillis() = 10_000L
        override fun setShuffleMode(isShuffle: Boolean) = Unit
        override fun setRepeatMode(isRepeat: Boolean) = Unit
        override fun setPlaybackSpeed(speed: Float) = Unit
        override fun setPlaybackPitchSemitone(semitone: Int) = Unit
        override fun addItem(item: IosMediaItem, atStart: Boolean) = Unit
        override fun removeItem(mediaId: String) = Unit
        override fun replaceItem(mediaId: String, item: IosMediaItem) = Unit
    }
}
