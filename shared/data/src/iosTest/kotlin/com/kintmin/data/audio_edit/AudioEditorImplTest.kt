package com.kintmin.data.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditPart
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatFloat32
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class AudioEditorImplTest {

    private companion object {

        const val SAMPLE_RATE = 44_100.0
        const val SOURCE_SECONDS = 10

        /** 원본은 1초마다 주파수가 바뀌어, 출력의 어느 지점이 원본의 몇 초인지 주파수로 가린다. */
        fun blockFrequency(block: Int): Double = 200.0 + 150.0 * block
    }

    private val fileSystem = FileSystem.SYSTEM
    private val root: Path = (NSTemporaryDirectory() + NSUUID().UUIDString).toPath()
    private val sourcePath get() = (root / "source.m4a").toString()
    private val outputPath get() = (root / "output.m4a").toString()
    private val editor = AudioEditorImpl()

    @BeforeTest
    fun setup() {
        fileSystem.createDirectories(root)
        writeSineSource(sourcePath)
    }

    @AfterTest
    fun tearDown() {
        fileSystem.deleteRecursively(root)
    }

    @Test
    fun `원본 길이를 잰다`() = runTest {
        assertEquals(SOURCE_SECONDS * 1000L, editor.measureDurationMs(sourcePath).getOrThrow())
    }

    @Test
    fun `앞뒤와 중간의 무음과 순서를 바꾼 조각이 그대로 렌더된다`() = runTest(timeout = 1.minutes) {
        val parts = listOf(
            AudioEditPart.Silence(1_500),
            AudioEditPart.Audio(startMs = 2_300, endMs = 4_700),
            AudioEditPart.Silence(1_000),
            AudioEditPart.Audio(startMs = 7_100, endMs = 8_000),
            AudioEditPart.Audio(startMs = 500, endMs = 1_200),
            AudioEditPart.Silence(800),
        )

        val durationMs = editor.render(sourcePath, parts, outputPath).getOrThrow()

        assertEquals(7_300L, durationMs)
        val samples = decode(outputPath)
        var partStartMs = 0L
        for (part in parts) {
            // 경계 근처와 원본의 주파수가 바뀌는 지점은 창이 두 소리에 걸쳐 건너뛴다
            for (offsetMs in 100L until part.durationMs - 100 step 100) {
                val window = window(samples, centerMs = partStartMs + offsetMs)
                when (part) {
                    is AudioEditPart.Silence -> assertTrue(rms(window) < 0.01f, "${partStartMs + offsetMs}ms 는 무음이어야 한다")
                    is AudioEditPart.Audio -> {
                        val sourceMs = part.startMs + offsetMs
                        if (sourceMs % 1000 !in 60..940) continue
                        assertEquals((sourceMs / 1000).toInt(), nearestBlock(window), "${partStartMs + offsetMs}ms 의 원본 위치")
                    }
                }
            }
            partStartMs += part.durationMs
        }
    }

    @Test
    fun `원본 끝을 넘는 조각은 모자란 만큼 무음으로 채운다`() = runTest {
        val durationMs = editor.render(sourcePath, listOf(AudioEditPart.Audio(startMs = 9_500, endMs = 10_500)), outputPath)
            .getOrThrow()

        assertEquals(1_000L, durationMs)
        val samples = decode(outputPath)
        assertTrue(rms(window(samples, centerMs = 250)) > 0.2f)
        assertTrue(rms(window(samples, centerMs = 750)) < 0.01f)
    }

    @Test
    fun `원본을 열지 못하면 실패하고 출력 파일을 남기지 않는다`() = runTest {
        val missingPath = (root / "missing.m4a").toString()

        val result = editor.render(missingPath, listOf(AudioEditPart.Audio(startMs = 0, endMs = 1_000)), outputPath)

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.startsWith("음원을 열지 못했습니다. ("), "NSError 원인이 붙어야 한다: $message")
        assertFalse(fileSystem.exists(outputPath.toPath()))
        assertTrue(editor.measureDurationMs(missingPath).isFailure)
    }

    @Test
    fun `소리 조각이 없으면 실패하고 출력 파일을 남기지 않는다`() = runTest {
        val result = editor.render(sourcePath, listOf(AudioEditPart.Silence(1_000)), outputPath)

        assertTrue(result.isFailure)
        assertFalse(fileSystem.exists(outputPath.toPath()))
    }

    @Test
    fun `파형은 버킷마다 최대 진폭을 담고 마지막에 다 읽었다고 알린다`() = runTest(timeout = 1.minutes) {
        val waveforms = editor.extractWaveform(sourcePath, bucketCount = 100).toList()

        val last = waveforms.last()
        assertTrue(last.isComplete)
        assertEquals(100, last.peaks.size)
        // 원본은 진폭 0.5 사인파다. AAC 손실을 감안해 범위로 본다.
        assertTrue(last.peaks.all { it in 14_000..19_000 }, "peaks=${last.peaks}")
    }

    private fun writeSineSource(path: String) {
        val format = AVAudioFormat(standardFormatWithSampleRate = SAMPLE_RATE, channels = 2u)
        val frameCount = (SAMPLE_RATE * SOURCE_SECONDS).toInt()
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frameCount.toUInt())
        buffer.setFrameLength(frameCount.toUInt())
        var phase = 0.0
        for (i in 0 until frameCount) {
            phase += 2 * PI * blockFrequency(i / SAMPLE_RATE.toInt()) / SAMPLE_RATE
            val value = (0.5 * sin(phase)).toFloat()
            buffer.floatChannelData!![0]!![i] = value
            buffer.floatChannelData!![1]!![i] = value
        }
        val file = AVAudioFile(
            forWriting = NSURL.fileURLWithPath(path),
            settings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to SAMPLE_RATE,
                AVNumberOfChannelsKey to 2,
                AVEncoderBitRateKey to 128_000,
            ),
            commonFormat = AVAudioPCMFormatFloat32,
            interleaved = false,
            error = null,
        )
        file.writeFromBuffer(buffer, error = null)
        file.close()
    }

    private fun decode(path: String): FloatArray {
        val file = AVAudioFile(forReading = NSURL.fileURLWithPath(path), error = null)
        val buffer = AVAudioPCMBuffer(pCMFormat = file.processingFormat, frameCapacity = file.length.toUInt())
        file.readIntoBuffer(buffer, error = null)
        file.close()
        val channel = buffer.floatChannelData!![0]!!
        return FloatArray(buffer.frameLength.toInt()) { channel[it] }
    }

    private fun window(samples: FloatArray, centerMs: Long, widthMs: Long = 80): List<Float> {
        val center = (centerMs * SAMPLE_RATE / 1000).toInt()
        val half = (widthMs * SAMPLE_RATE / 2000).toInt()
        return samples.slice((center - half).coerceAtLeast(0) until (center + half).coerceAtMost(samples.size))
    }

    private fun rms(window: List<Float>): Float = sqrt(window.sumOf { (it * it).toDouble() } / window.size).toFloat()

    /** 영점 교차로 잰 주파수에 가장 가까운 원본 블록(초). */
    private fun nearestBlock(window: List<Float>): Int {
        val crossings = window.zipWithNext().count { (a, b) -> (a < 0) != (b < 0) }
        val frequency = crossings / 2.0 / (window.size / SAMPLE_RATE)
        return (0 until SOURCE_SECONDS).minBy { abs(blockFrequency(it) - frequency) }
    }
}
