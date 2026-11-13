package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditFrameSpan
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.toFrameSpans
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeCompletionDataPlayedBack
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memset

/**
 * `AVPlayer` + 컴포지션을 쓰지 않는다 — 트랙 끝에 넣은 빈 구간(끝 무음)이 재생되지 않는다.
 * 모든 호출과 상태는 메인 스레드에서 다룬다.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosAudioPreviewPlayer : AudioPreviewPlayer {

    private companion object {

        /** 무음은 이 길이의 0 버퍼를 되풀이해 스케줄한다. 긴 무음을 한 버퍼로 만들면 메모리를 크게 잡는다. */
        const val SILENCE_CHUNK_SECONDS = 1
    }

    private var engine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var playStartAtMs = 0L
    private var isPlayingState = false

    /** 멈춘 뒤에 이전 재생의 완료 콜백이 와도 새 재생을 끝난 것으로 표시하지 않게 재생마다 올린다. */
    private var playGeneration = 0

    /** 끼어들기나 출력 장치 변경으로 엔진이 저절로 멈추면 완료 콜백이 오지 않으므로 엔진 상태도 본다. */
    override val isPlaying: Boolean
        get() = isPlayingState && engine?.running == true

    override val currentPositionMs: Long?
        get() {
            val node = playerNode ?: return null
            val nodeTime = node.lastRenderTime ?: return null
            val playerTime = node.playerTimeForNodeTime(nodeTime) ?: return null
            if (playerTime.sampleRate <= 0) return null
            return playStartAtMs + (playerTime.sampleTime * 1000.0 / playerTime.sampleRate).toLong().coerceAtLeast(0)
        }

    override fun play(audioFileFullPath: String, parts: List<AudioEditPart>, startAtMs: Long) {
        release()

        val file = withNSError { error -> AVAudioFile(forReading = NSURL.fileURLWithPath(audioFileFullPath), error = error) }
            ?: return
        val format = file.processingFormat
        val spans = parts.toFrameSpans(sampleRate = format.sampleRate, sourceFrameCount = file.length, startAtMs = startAtMs)
        if (spans.isEmpty()) return

        val newEngine = AVAudioEngine()
        val node = AVAudioPlayerNode()
        newEngine.attachNode(node)
        newEngine.connect(node, to = newEngine.mainMixerNode, format = format)

        val generation = ++playGeneration
        val onPlayedBack: (Long) -> Unit = {
            // 엔진이 돌고 있으면 OS 가 앱을 재생 중으로 보고 백그라운드에서 서스펜드하지 않는다
            dispatch_async(dispatch_get_main_queue()) {
                if (playGeneration == generation) release()
            }
        }
        scheduleSpans(node, file, format, spans, onPlayedBack)

        // 메인 재생 엔진은 처음 재생할 때에야 세션을 잡는다. 그 전이면 무음 스위치에 묻힌다.
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryPlayback, error = null)
            setActive(true, error = null)
        }
        newEngine.prepare()
        val isStarted = withNSError { error -> newEngine.startAndReturnError(error) } == true
        if (!isStarted) return

        node.play()
        engine = newEngine
        playerNode = node
        playStartAtMs = startAtMs
        isPlayingState = true
    }

    private fun scheduleSpans(
        node: AVAudioPlayerNode,
        file: AVAudioFile,
        format: AVAudioFormat,
        spans: List<AudioEditFrameSpan>,
        onPlayedBack: (Long) -> Unit,
    ) {
        val silenceChunkFrameCount = (format.sampleRate * SILENCE_CHUNK_SECONDS).toLong()
        val silenceChunk by lazy { silentBuffer(format, silenceChunkFrameCount) }
        val schedules = mutableListOf<(((Long) -> Unit)?) -> Unit>()

        fun addSilence(frameCount: Long) {
            var remaining = frameCount
            while (remaining > 0) {
                val chunkFrameCount = minOf(remaining, silenceChunkFrameCount)
                remaining -= chunkFrameCount
                val buffer = if (chunkFrameCount == silenceChunkFrameCount) silenceChunk else silentBuffer(format, chunkFrameCount)
                schedules += { handler ->
                    node.scheduleBuffer(
                        buffer,
                        atTime = null,
                        options = 0u,
                        completionCallbackType = AVAudioPlayerNodeCompletionDataPlayedBack,
                        completionHandler = handler,
                    )
                }
            }
        }

        for (span in spans) {
            when (span) {
                is AudioEditFrameSpan.Audio -> schedules += { handler ->
                    node.scheduleSegment(
                        file,
                        startingFrame = span.sourceStartFrame,
                        frameCount = span.frameCount.toUInt(),
                        atTime = null,
                        completionCallbackType = AVAudioPlayerNodeCompletionDataPlayedBack,
                        completionHandler = handler,
                    )
                }

                is AudioEditFrameSpan.Silence -> addSilence(span.frameCount)
            }
        }

        schedules.forEachIndexed { index, schedule -> schedule(onPlayedBack.takeIf { index == schedules.lastIndex }) }
    }

    private fun silentBuffer(format: AVAudioFormat, frameCount: Long): AVAudioPCMBuffer =
        AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frameCount.toUInt()).apply {
            setFrameLength(frameCount.toUInt())
            val channels = floatChannelData!!
            for (channel in 0 until format.channelCount.toInt()) {
                memset(channels[channel], 0, frameCount.toULong() * Float.SIZE_BYTES.toULong())
            }
        }

    /** ViewModel 은 다시 들을 때 항상 기준선에서 [play] 를 새로 부르므로 이어 듣기 위치를 남기지 않는다. */
    override fun pause() = release()

    override fun release() {
        playGeneration++
        playerNode?.stop()
        engine?.stop()
        playerNode = null
        engine = null
        isPlayingState = false
    }

    /**
     * 실패하면 null. Cocoa 규약상 error 는 반환값이 false 일 때만 유효하고,
     * Kotlin/Native 는 init 이 nil 을 돌려주면 예외로 올린다.
     */
    private inline fun <T> withNSError(block: (CPointer<ObjCObjectVar<NSError?>>) -> T): T? = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val result = try {
            block(error.ptr)
        } catch (e: Exception) {
            return@memScoped null
        }
        result.takeUnless { it == false }
    }
}
