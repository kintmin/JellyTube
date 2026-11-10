package com.kintmin.ios.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class IosMediaControllerManagerTest {

    private val bridge = FakeMediaControllerBridge()
    private val manager = IosMediaControllerManager(bridge)

    @Test
    fun `구간반복은 시작점 끝점 해제로 돈다`() {
        // Android 구현(MediaControllerManagerImpl)과 단계가 같아야 한다.
        bridge.positionMillis = 1_000
        manager.updateRepeatRange()
        assertEquals(1_000.milliseconds, manager.repeatRangeState.startDuration)
        assertNull(manager.repeatRangeState.endDuration)

        bridge.positionMillis = 3_000
        manager.updateRepeatRange()
        assertEquals(3_000.milliseconds, manager.repeatRangeState.endDuration)

        manager.updateRepeatRange()
        assertNull(manager.repeatRangeState.startDuration)
        assertNull(manager.repeatRangeState.endDuration)
    }

    @Test
    fun `끝점이 시작점보다 앞이면 실패로 돌려준다`() {
        bridge.positionMillis = 3_000
        manager.updateRepeatRange()

        bridge.positionMillis = 1_000
        assertTrue(manager.updateRepeatRange().isFailure)
        assertNull(manager.repeatRangeState.endDuration)
    }

    @Test
    fun `곡이 바뀌면 구간이 풀린다`() {
        bridge.positionMillis = 1_000
        manager.updateRepeatRange()
        bridge.positionMillis = 3_000
        manager.updateRepeatRange()

        bridge.playing = item("2")

        assertNull(manager.repeatRangeState.startDuration)
        assertEquals(null, manager.repeatRangeState.mediaId)
    }

    @Test
    fun `끝점을 넘으면 시작점으로 되돌린다`() {
        bridge.positionMillis = 1_000
        manager.updateRepeatRange()
        bridge.positionMillis = 3_000
        manager.updateRepeatRange()

        bridge.positionMillis = 3_001
        manager.repeatRangeIfNeeded()

        assertEquals(listOf(1_000L), bridge.seekCalls)
    }

    @Test
    fun `구간이 덜 잡혔으면 되돌리지 않는다`() {
        bridge.positionMillis = 1_000
        manager.updateRepeatRange()

        bridge.positionMillis = 9_000
        manager.repeatRangeIfNeeded()

        assertEquals(emptyList(), bridge.seekCalls)
    }

    private fun item(mediaId: String) = IosMediaItem(
        mediaId = mediaId,
        fileFullPath = "/tmp/$mediaId.m4a",
        title = "title",
        description = "",
        artist = "artist",
        durationMillis = 10_000,
        artworkFileFullPath = null,
    )

    private inner class FakeMediaControllerBridge : IosMediaControllerBridge {
        var positionMillis = 0L
        var playing: IosMediaItem? = item("1")
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
