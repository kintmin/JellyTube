package com.kintmin.data.audio_edit

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioWaveform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

internal class AudioEditorImpl constructor(
    private val appContext: Context,
) : AudioEditor {

    private companion object {

        /** 디코더 입출력 대기 시간. 0이면 바쁜 대기가 되고 너무 길면 끝까지 블록된다. */
        const val CODEC_TIMEOUT_US = 10_000L

        /** 길이를 못 읽는 음원의 파형을 뽑을 때 쓰는 상한. */
        const val FALLBACK_DURATION_US = 10 * 60 * 1_000_000L

        /** 읽는 중간 결과를 내보내는 최소 간격. 너무 잦으면 매번 배열 복사와 다시 그리기가 일어난다. */
        const val PROGRESS_EMIT_INTERVAL_MS = 300L
    }

    override fun extractWaveform(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Flow<AudioWaveform> = flow {
        require(bucketCount > 0) { "파형 버킷 수는 1 이상이어야 합니다." }
        decodePeaks(audioFileFullPath = audioFileFullPath, bucketCount = bucketCount)
    }.flowOn(Dispatchers.IO)

    /**
     * 음원을 PCM으로 디코딩하면서 버킷별 최대 진폭만 남긴다. 읽는 도중에도 주기적으로 내보낸다.
     *
     * 샘플을 전부 메모리에 들고 있지 않아 길이에 상관없이 메모리 사용이 일정하다.
     * 버킷 인덱스는 샘플의 표시 시각으로 계산해서, 가변 비트레이트여도 시간축이 어긋나지 않는다.
     * 수집이 취소되면 emit에서 멈추고 finally에서 코덱을 정리한다.
     */
    private suspend fun FlowCollector<AudioWaveform>.decodePeaks(audioFileFullPath: String, bucketCount: Int) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(audioFileFullPath)

            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: throw Exception("오디오 트랙을 찾을 수 없습니다.")

            val inputFormat = extractor.getTrackFormat(trackIndex)
            extractor.selectTrack(trackIndex)

            val durationUs = inputFormat
                .takeIf { it.containsKey(MediaFormat.KEY_DURATION) }
                ?.getLong(MediaFormat.KEY_DURATION)
                ?.takeIf { it > 0 }
                ?: FALLBACK_DURATION_US

            val mimeType = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw Exception("오디오 포맷을 확인할 수 없습니다.")

            codec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            val peaks = IntArray(bucketCount)
            val bufferInfo = MediaCodec.BufferInfo()
            var isInputDone = false
            var loadedBucketCount = 0
            var lastEmitTimeMs = SystemClock.elapsedRealtime()

            while (true) {
                if (!isInputDone) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            isInputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
                if (outputIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                        val bucketIndex = (bufferInfo.presentationTimeUs * bucketCount / durationUs)
                            .toInt()
                            .coerceIn(0, bucketCount - 1)

                        val peak = peakOf(
                            buffer = outputBuffer,
                            offset = bufferInfo.offset,
                            size = bufferInfo.size,
                            pcmEncoding = codec.outputFormat.pcmEncoding(),
                        )
                        if (peak > peaks[bucketIndex]) peaks[bucketIndex] = peak
                        // 디코딩은 앞에서부터 순서대로라 지금 버킷 앞까지는 다 채워졌다
                        loadedBucketCount = maxOf(loadedBucketCount, bucketIndex)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)

                    val nowMs = SystemClock.elapsedRealtime()
                    if (nowMs - lastEmitTimeMs >= PROGRESS_EMIT_INTERVAL_MS) {
                        lastEmitTimeMs = nowMs
                        emit(AudioWaveform(peaks = peaks.toList(), loadedBucketCount = loadedBucketCount))
                    }

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && isInputDone) {
                    // 입력이 끝났는데도 출력이 없으면 더 기다릴 이유가 없다
                    break
                }
            }

            emit(AudioWaveform(peaks = peaks.toList(), loadedBucketCount = bucketCount))
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /** 디코더 출력의 PCM 인코딩. 명시가 없으면 16비트다. */
    private fun MediaFormat.pcmEncoding(): Int =
        takeIf { it.containsKey(MediaFormat.KEY_PCM_ENCODING) }
            ?.getInteger(MediaFormat.KEY_PCM_ENCODING)
            ?: android.media.AudioFormat.ENCODING_PCM_16BIT

    /** 버퍼 구간의 최대 절대 진폭. 16비트 기준(0..32767)으로 맞춘다. */
    private fun peakOf(buffer: ByteBuffer, offset: Int, size: Int, pcmEncoding: Int): Int {
        val view = buffer.duplicate().apply {
            position(offset)
            limit(offset + size)
            order(ByteOrder.nativeOrder())
        }

        var peak = 0
        when (pcmEncoding) {
            android.media.AudioFormat.ENCODING_PCM_FLOAT -> {
                val floatBuffer = view.asFloatBuffer()
                while (floatBuffer.hasRemaining()) {
                    val value = (abs(floatBuffer.get()) * Short.MAX_VALUE).toInt()
                    if (value > peak) peak = value
                }
            }

            android.media.AudioFormat.ENCODING_PCM_8BIT -> {
                while (view.hasRemaining()) {
                    // 8비트 PCM은 unsigned라 128이 무음 기준점이다
                    val value = abs((view.get().toInt() and 0xFF) - 128) * 256
                    if (value > peak) peak = value
                }
            }

            else -> {
                val shortBuffer = view.asShortBuffer()
                while (shortBuffer.hasRemaining()) {
                    val value = abs(shortBuffer.get().toInt())
                    if (value > peak) peak = value
                }
            }
        }
        return peak
    }

    override suspend fun render(
        sourceFileFullPath: String,
        parts: List<AudioEditPart>,
        outputFileFullPath: String,
    ): Result<Long> = runCatching {
        require(parts.any { it is AudioEditPart.Audio }) { "소리가 남은 조각이 없습니다." }
        require(parts.all { it.durationMs > 0 }) { "길이가 0인 조각은 편집할 수 없습니다." }

        val composition = buildComposition(sourceFileFullPath, parts)

        // Transformer는 Looper가 있는 스레드에서 만들고 시작해야 한다
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val transformer = Transformer.Builder(appContext)
                    // 입력이 이미 AAC면 재인코딩 없이 컨테이너만 바꿔 담는다(무손실)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            continuation.resume(Unit)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            continuation.resumeWithException(exportException)
                        }
                    })
                    .build()

                continuation.invokeOnCancellation {
                    runCatching { transformer.cancel() }
                }
                transformer.start(composition, outputFileFullPath)
            }
        }

        measureDurationMs(outputFileFullPath)
    }.onFailure {
        // 실패한 출력 파일을 남겨두면 고아 파일이 된다
        runCatching { File(outputFileFullPath).delete() }
    }

    /**
     * 같은 파일의 구간과 무음을 시간순으로 이어붙인 Composition.
     *
     * 소리 조각은 클리핑만 다른 EditedMediaItem으로, 무음은 시퀀스의 gap으로 넣으면
     * Transformer가 순서대로 연결해준다.
     */
    private fun buildComposition(
        sourceFileFullPath: String,
        parts: List<AudioEditPart>,
    ): Composition {
        val sourceUri = Uri.fromFile(File(sourceFileFullPath))

        // 시퀀스가 오디오 트랙만 낸다고 밝혀 둔다. 그래야 gap이 무음 오디오로 채워지고,
        // 맨 앞이 무음이어도 출력에 오디오 트랙이 생긴다.
        val sequenceBuilder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))

        parts.forEach { part ->
            when (part) {
                is AudioEditPart.Audio -> {
                    val mediaItem = MediaItem.Builder()
                        .setUri(sourceUri)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(part.startMs)
                                .setEndPositionMs(part.endMs)
                                .build()
                        )
                        .build()

                    sequenceBuilder.addItem(
                        EditedMediaItem.Builder(mediaItem)
                            .setRemoveVideo(true)
                            .build()
                    )
                }

                is AudioEditPart.Silence -> sequenceBuilder.addGap(part.durationMs * 1_000)
            }
        }

        return Composition.Builder(sequenceBuilder.build()).build()
    }

    /** 만들어진 파일의 실제 길이(ms). */
    private fun measureDurationMs(fileFullPath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(fileFullPath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: throw Exception("편집한 음원의 길이를 확인할 수 없습니다.")
        } finally {
            runCatching { retriever.release() }
        }
    }
}
