package com.kintmin.platform.audio_preview

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SilenceMediaSource
import com.kintmin.domain.audio_edit.model.AudioEditPart
import java.io.File

@OptIn(UnstableApi::class)
internal class AudioPreviewPlayerImpl constructor(
    private val appContext: Context,
) : AudioPreviewPlayer {

    private var player: ExoPlayer? = null

    /** 재생 중인 조각마다 타임라인 시작 시각 */
    private var partStartMsList: List<Long> = emptyList()

    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override val currentPositionMs: Long?
        get() {
            val currentPlayer = player ?: return null
            val partStartMs = partStartMsList.getOrNull(currentPlayer.currentMediaItemIndex) ?: return null
            return partStartMs + currentPlayer.currentPosition
        }

    override fun play(audioFileFullPath: String, parts: List<AudioEditPart>, startAtMs: Long) {
        if (parts.isEmpty()) return

        val currentPlayer = player ?: ExoPlayer.Builder(appContext).build().also { player = it }
        partStartMsList = parts.runningFold(0L) { startMs, part -> startMs + part.durationMs }.dropLast(1)

        val sourceUri = Uri.fromFile(File(audioFileFullPath))
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
        currentPlayer.setMediaSources(
            parts.map { part -> part.toMediaSource(sourceUri, mediaSourceFactory) }
        )

        val startIndex = partStartMsList
            .indexOfLast { it <= startAtMs }
            .coerceAtLeast(0)
        val offsetMs = (startAtMs - partStartMsList[startIndex])
            .coerceIn(0L, parts[startIndex].durationMs)
        currentPlayer.seekTo(startIndex, offsetMs)

        currentPlayer.prepare()
        currentPlayer.play()
    }

    private fun AudioEditPart.toMediaSource(
        sourceUri: Uri,
        mediaSourceFactory: DefaultMediaSourceFactory,
    ): MediaSource = when (this) {
        is AudioEditPart.Audio -> mediaSourceFactory.createMediaSource(
            MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
        )

        is AudioEditPart.Silence -> SilenceMediaSource(durationMs * 1_000)
    }

    override fun pause() {
        player?.pause()
    }

    override fun release() {
        player?.release()
        player = null
        partStartMsList = emptyList()
    }
}
