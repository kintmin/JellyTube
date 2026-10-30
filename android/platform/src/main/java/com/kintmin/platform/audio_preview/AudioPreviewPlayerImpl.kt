package com.kintmin.platform.audio_preview

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.kintmin.domain.audio_edit.model.AudioEditSegment
import java.io.File

internal class AudioPreviewPlayerImpl constructor(
    private val appContext: Context,
) : AudioPreviewPlayer {

    private var player: ExoPlayer? = null

    /** 재생 중인 구간 목록. 플레이어 위치를 원본 타임라인으로 환산할 때 쓴다. */
    private var playingSegments: List<AudioEditSegment> = emptyList()

    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override val currentPositionMs: Long?
        get() {
            val currentPlayer = player ?: return null
            val segment = playingSegments.getOrNull(currentPlayer.currentMediaItemIndex) ?: return null
            return segment.startMs + currentPlayer.currentPosition
        }

    override fun play(audioFileFullPath: String, segments: List<AudioEditSegment>) {
        if (segments.isEmpty()) return

        val currentPlayer = player ?: ExoPlayer.Builder(appContext).build().also { player = it }
        playingSegments = segments

        val sourceUri = Uri.fromFile(File(audioFileFullPath))
        currentPlayer.setMediaItems(
            segments.map { segment ->
                MediaItem.Builder()
                    .setUri(sourceUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(segment.startMs)
                            .setEndPositionMs(segment.endMs)
                            .build()
                    )
                    .build()
            }
        )
        currentPlayer.prepare()
        currentPlayer.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun release() {
        player?.release()
        player = null
        playingSegments = emptyList()
    }
}
