package com.kintmin.data.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditFrameSpan
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.domain.audio_edit.model.toFrameSpans
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioConverter
import platform.AVFAudio.AVAudioConverterInputStatus_EndOfStream
import platform.AVFAudio.AVAudioConverterInputStatus_HaveData
import platform.AVFAudio.AVAudioConverterOutputStatus_EndOfStream
import platform.AVFAudio.AVAudioConverterOutputStatus_Error
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatFloat32
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.Accelerate.vDSP_maxmgv
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.posix.memset
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class AudioEditorImpl : AudioEditor {

    private companion object {

        const val CHUNK_FRAME_COUNT = 32_768u

        /** AAC 인코더가 받는 최대 샘플레이트. 넘으면 쓰기가 -50 으로 거절된다. */
        const val MAX_AAC_SAMPLE_RATE = 48_000.0

        /** 읽는 중간 결과를 내보내는 최소 간격. 너무 잦으면 매번 배열 복사와 다시 그리기가 일어난다. */
        val PROGRESS_EMIT_INTERVAL = 300.milliseconds
    }

    /** 메인 재생 엔진과 같은 방식으로 잰다. 그래야 탐색바 길이와 어긋나지 않는다. */
    override suspend fun measureDurationMs(audioFileFullPath: String): Result<Long> =
        withContext(Dispatchers.IO) { runCatching { readDurationMs(audioFileFullPath) } }

    override fun extractWaveform(
        audioFileFullPath: String,
        bucketCount: Int,
    ): Flow<AudioWaveform> = flow {
        require(bucketCount > 0) { "파형 버킷 수는 1 이상이어야 합니다." }

        openForReading(audioFileFullPath).use { file ->
            val totalFrameCount = file.length
            if (totalFrameCount <= 0) throw Exception("음원 길이를 확인할 수 없습니다.")

            val channelCount = file.processingFormat.channelCount.toInt()
            val buffer = pcmBuffer(file.processingFormat)
            val buckets = WaveformBuckets(totalFrameCount, bucketCount)
            val peaks = IntArray(bucketCount)
            var framePosition = 0L
            var lastEmitMark = TimeSource.Monotonic.markNow()

            while (true) {
                file.readChunk(buffer, CHUNK_FRAME_COUNT)
                val readFrameCount = buffer.frameLength.toLong()
                if (readFrameCount == 0L) break

                val channels = buffer.floatChannelData!!
                var offset = 0L
                while (offset < readFrameCount) {
                    val bucketIndex = buckets.indexOf(framePosition + offset)
                    val segmentFrameCount = (buckets.endFrameOf(bucketIndex) - framePosition - offset)
                        .coerceIn(1L, readFrameCount - offset)

                    val peak = (0 until channelCount).maxOf { channel ->
                        maxMagnitude((channels[channel]!! + offset)!!, segmentFrameCount)
                    }
                    val scaled = (peak.coerceAtMost(1f) * AudioWaveform.FULL_SCALE_PEAK).toInt()
                    if (scaled > peaks[bucketIndex]) peaks[bucketIndex] = scaled
                    offset += segmentFrameCount
                }
                framePosition += readFrameCount

                if (lastEmitMark.elapsedNow() >= PROGRESS_EMIT_INTERVAL) {
                    lastEmitMark = TimeSource.Monotonic.markNow()
                    emit(AudioWaveform(peaks = peaks.toList(), loadedBucketCount = buckets.loadedCountAfter(framePosition)))
                }
            }

            emit(AudioWaveform(peaks = peaks.toList(), loadedBucketCount = bucketCount))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun render(
        sourceFileFullPath: String,
        parts: List<AudioEditPart>,
        outputFileFullPath: String,
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            require(parts.any { it is AudioEditPart.Audio }) { "소리가 남은 조각이 없습니다." }
            require(parts.all { it.durationMs > 0 }) { "길이가 0인 조각은 편집할 수 없습니다." }

            writeEditedAudio(sourceFileFullPath, parts, outputFileFullPath)
            Result.success(readDurationMs(outputFileFullPath))
        } catch (e: CancellationException) {
            deleteFile(outputFileFullPath)
            throw e
        } catch (e: Throwable) {
            deleteFile(outputFileFullPath)
            Result.failure(e)
        }
    }

    private suspend fun writeEditedAudio(
        sourceFileFullPath: String,
        parts: List<AudioEditPart>,
        outputFileFullPath: String,
    ) {
        openForReading(sourceFileFullPath).use { input ->
            val sourceFormat = input.processingFormat
            val chunks = EditedPcmChunks(
                input = input,
                spans = parts.toFrameSpans(sampleRate = sourceFormat.sampleRate, sourceFrameCount = input.length),
            )
            openForWriting(outputFileFullPath, encodableFormatOf(sourceFormat)).use { output ->
                if (output.processingFormat.isEqual(sourceFormat)) {
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        output.writeChunk(chunks.next() ?: break)
                    }
                } else {
                    writeConverted(chunks, sourceFormat, output.processingFormat, output)
                }
            }
        }
    }

    private fun encodableFormatOf(source: AVAudioFormat): AVAudioFormat =
        if (source.sampleRate <= MAX_AAC_SAMPLE_RATE && source.channelCount <= 2u) {
            source
        } else {
            AVAudioFormat(
                standardFormatWithSampleRate = minOf(source.sampleRate, MAX_AAC_SAMPLE_RATE),
                channels = minOf(source.channelCount, 2u),
            )
        }

    private suspend fun writeConverted(
        chunks: EditedPcmChunks,
        sourceFormat: AVAudioFormat,
        outputFormat: AVAudioFormat,
        output: AVAudioFile,
    ) {
        val converter = AVAudioConverter(fromFormat = sourceFormat, toFormat = outputFormat)
        converter.downmix = true
        val buffer = pcmBuffer(outputFormat)
        var isInputEnded = false
        // ObjC 블록 밖으로 예외가 나가면 Kotlin/Native 가 앱을 죽이므로 잡아 두었다가 밖에서 던진다
        var inputError: Throwable? = null

        while (true) {
            currentCoroutineContext().ensureActive()
            val status = throwingNSError("음원을 변환하지 못했습니다.") { error ->
                converter.convertToBuffer(buffer, error = error) { _, inputStatus ->
                    val chunk = if (isInputEnded) null else runCatching { chunks.next() }.onFailure { inputError = it }.getOrNull()
                    if (chunk == null) isInputEnded = true
                    inputStatus!!.pointed.value =
                        if (chunk == null) AVAudioConverterInputStatus_EndOfStream else AVAudioConverterInputStatus_HaveData
                    chunk
                }
            }
            inputError?.let { throw it }
            if (status == AVAudioConverterOutputStatus_Error) throw Exception("음원을 변환하지 못했습니다.")
            if (buffer.frameLength > 0u) output.writeChunk(buffer)
            if (status == AVAudioConverterOutputStatus_EndOfStream) break
        }
    }

    private inner class EditedPcmChunks(
        private val input: AVAudioFile,
        spans: List<AudioEditFrameSpan>,
    ) {
        private val pendingSpans = ArrayDeque(spans)
        private var currentSpan: AudioEditFrameSpan? = null
        private var remainingFrameCount = 0L

        /** 변환기가 앞 청크를 아직 들고 있을 수 있어 청크마다 새 버퍼를 준다. */
        fun next(): AVAudioPCMBuffer? {
            while (remainingFrameCount == 0L) {
                val span = pendingSpans.removeFirstOrNull() ?: return null
                currentSpan = span
                remainingFrameCount = span.frameCount
                if (span is AudioEditFrameSpan.Audio) input.framePosition = span.sourceStartFrame
            }
            val buffer = pcmBuffer(input.processingFormat)
            val frameCount = minOf(remainingFrameCount, CHUNK_FRAME_COUNT.toLong()).toUInt()
            when (currentSpan) {
                is AudioEditFrameSpan.Audio -> {
                    input.readChunk(buffer, frameCount)
                    if (buffer.frameLength == 0u) throw Exception("음원을 끝까지 읽지 못했습니다.")
                }

                else -> {
                    buffer.setFrameLength(frameCount)
                    for (channel in 0 until input.processingFormat.channelCount.toInt()) {
                        memset(buffer.floatChannelData!![channel], 0, frameCount.toULong() * Float.SIZE_BYTES.toULong())
                    }
                }
            }
            remainingFrameCount -= buffer.frameLength.toLong()
            return buffer
        }
    }

    private fun readDurationMs(fileFullPath: String): Long =
        openForReading(fileFullPath).use { file ->
            val sampleRate = file.processingFormat.sampleRate
            if (file.length <= 0 || sampleRate <= 0) throw Exception("음원 길이를 확인할 수 없습니다.")
            (file.length * 1000.0 / sampleRate).toLong()
        }

    private fun openForReading(fileFullPath: String): AVAudioFile = throwingNSError("음원을 열지 못했습니다.") { error ->
        AVAudioFile(forReading = NSURL.fileURLWithPath(fileFullPath), error = error)
    }

    private fun openForWriting(fileFullPath: String, format: AVAudioFormat): AVAudioFile = throwingNSError("편집본 파일을 만들지 못했습니다.") { error ->
        AVAudioFile(
            forWriting = NSURL.fileURLWithPath(fileFullPath),
            settings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to format.sampleRate,
                AVNumberOfChannelsKey to format.channelCount,
                AVEncoderAudioQualityKey to AVAudioQualityHigh,
            ),
            commonFormat = AVAudioPCMFormatFloat32,
            interleaved = false,
            error = error,
        )
    }

    private fun pcmBuffer(format: AVAudioFormat): AVAudioPCMBuffer =
        AVAudioPCMBuffer(pCMFormat = format, frameCapacity = CHUNK_FRAME_COUNT)

    /** 파일 끝이면 [buffer] 길이가 0이 된다. `readIntoBuffer` 는 끝에서 빈 버퍼가 아니라 실패를 돌려준다. */
    private fun AVAudioFile.readChunk(buffer: AVAudioPCMBuffer, frameCount: UInt) {
        val remainingFrameCount = length - framePosition
        if (remainingFrameCount <= 0) {
            buffer.setFrameLength(0u)
            return
        }
        val readFrameCount = minOf(frameCount.toLong(), remainingFrameCount).toUInt()
        throwingNSError("음원을 읽지 못했습니다.") { error ->
            readIntoBuffer(buffer, frameCount = readFrameCount, error = error)
        }
    }

    private fun AVAudioFile.writeChunk(buffer: AVAudioPCMBuffer) {
        throwingNSError("편집본을 쓰지 못했습니다.") { error -> writeFromBuffer(buffer, error = error) }
    }

    /** 닫아야 출력 파일의 AAC 꼬리와 길이 정보가 기록된다. */
    private inline fun <T> AVAudioFile.use(block: (AVAudioFile) -> T): T =
        try {
            block(this)
        } finally {
            close()
        }

    /** Cocoa 규약상 error 는 반환값이 false 일 때만 유효하다. 성공한 호출도 error 를 채울 수 있다. */
    private inline fun <T> throwingNSError(failureMessage: String, block: (CPointer<ObjCObjectVar<NSError?>>) -> T): T =
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val result = try {
                block(error.ptr)
            } catch (e: Exception) {
                throw Exception(failureMessage + error.value.toDetail(), e)
            }
            if (result == false) throw Exception(failureMessage + error.value.toDetail())
            result
        }

    private fun NSError?.toDetail(): String = this?.let { " (${it.domain} ${it.code}: ${it.localizedDescription})" } ?: ""

    private fun maxMagnitude(samples: CPointer<FloatVar>, frameCount: Long): Float = memScoped {
        val result = alloc<FloatVar>()
        vDSP_maxmgv(samples, 1, result.ptr, frameCount.toULong())
        result.value
    }

    private fun deleteFile(fileFullPath: String) {
        runCatching { NSFileManager.defaultManager.removeItemAtPath(fileFullPath, error = null) }
    }
}
