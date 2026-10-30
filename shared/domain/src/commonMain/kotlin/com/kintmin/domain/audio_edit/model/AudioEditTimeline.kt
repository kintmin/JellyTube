package com.kintmin.domain.audio_edit.model

/**
 * 편집 화면의 타임라인. 원본에서 떼어 온 조각(clip)들의 배치와 결과물의 끝 위치를 들고 있다.
 *
 * - 시각은 모두 타임라인 기준이다. 조각마다 원본 구간과 타임라인 위치를 따로 가진다([AudioEditClip]).
 * - 조각이 없는 곳은 빈칸이고, 저장할 때 무음으로 들어간다.
 * - [endMs]가 결과물의 끝이다. 마지막 조각보다 뒤면 무음이 붙고, 앞이면 그 뒤 소리가 잘린다.
 *   조각을 옮기거나 늘려 끝을 넘기면 넘긴 만큼 끝이 따라 늘어난다. 줄어드는 쪽으로는 따라가지 않는다.
 * - 조각끼리는 겹칠 수 없고 0초 앞으로 갈 수 없다.
 *
 * 모든 조작은 새 인스턴스를 돌려준다. 규칙을 어기는 값은 가능한 범위로 맞추거나 무시한다.
 */
data class AudioEditTimeline(
    val originDurationMs: Long,
    /** 타임라인 위치 순으로 정렬되어 있고 서로 겹치지 않는다. */
    val clips: List<AudioEditClip>,
    val endMs: Long,
) {

    enum class Edge { Start, End }

    companion object {

        /** 이보다 짧은 조각은 재생 의미가 없어 만들지 않는다. */
        const val MIN_CLIP_MS = 100L

        /** 끝 표시를 늘릴 수 있는 상한(원본 길이 배수). 무음을 끝없이 붙이는 것을 막는다. */
        const val MAX_END_RATIO = 3L

        fun initial(originDurationMs: Long) = AudioEditTimeline(
            originDurationMs = originDurationMs,
            clips = if (originDurationMs > 0) {
                listOf(AudioEditClip(sourceStartMs = 0, sourceEndMs = originDurationMs, positionMs = 0))
            } else {
                emptyList()
            },
            endMs = originDurationMs,
        )
    }

    val maxEndMs: Long get() = (originDurationMs * MAX_END_RATIO).coerceAtLeast(MIN_CLIP_MS)

    val isDirty: Boolean get() = this != initial(originDurationMs)

    val hasAudio: Boolean get() = toParts().any { it is AudioEditPart.Audio }

    /** 타임라인 [ms] 위치에 있는 조각의 index. 빈칸이면 null. */
    fun clipIndexAt(ms: Long): Int? =
        clips.indexOfFirst { ms >= it.positionMs && ms < it.positionEndMs }.takeIf { it >= 0 }

    /** [atMs]에서 나눴을 때 양쪽이 모두 최소 길이 이상인 조각의 index. */
    private fun splittableClipIndex(atMs: Long): Int? =
        clips.indexOfFirst {
            atMs - it.positionMs >= MIN_CLIP_MS && it.positionEndMs - atMs >= MIN_CLIP_MS
        }.takeIf { it >= 0 }

    fun canSplit(atMs: Long): Boolean = splittableClipIndex(atMs) != null

    fun split(atMs: Long): AudioEditTimeline {
        val index = splittableClipIndex(atMs) ?: return this
        val clip = clips[index]
        val splitSourceMs = clip.sourceMsAt(atMs)
        return copy(
            clips = clips.toMutableList().apply {
                set(index, clip.copy(sourceEndMs = splitSourceMs))
                add(index + 1, clip.copy(sourceStartMs = splitSourceMs, positionMs = atMs))
            }
        )
    }

    fun delete(index: Int): AudioEditTimeline {
        if (index !in clips.indices) return this
        return copy(clips = clips.filterIndexed { clipIndex, _ -> clipIndex != index })
    }

    /**
     * 조각 한쪽 끝을 타임라인 [toMs]로 옮긴다. 바깥으로 끌면 원본 소리가 되살아나고, 안쪽으로 끌면 줄어든다.
     *
     * 이웃 조각과 겹치거나, 0초 앞으로 가거나, 원본에 없는 소리를 끌어오거나,
     * 최소 길이보다 짧아지거나, 끝 표시 상한을 넘지 않게 맞춘다.
     */
    fun resizeEdge(index: Int, edge: Edge, toMs: Long): AudioEditTimeline {
        if (index !in clips.indices) return this
        val clip = clips[index]
        // 최소 길이보다 짧은 원본이면 움직일 여지가 없다
        if (clip.durationMs < MIN_CLIP_MS) return this

        val resized = when (edge) {
            Edge.Start -> {
                val lowerMs = maxOf(
                    clips.getOrNull(index - 1)?.positionEndMs ?: 0L,
                    // 원본 0초보다 앞의 소리는 없다
                    clip.positionMs - clip.sourceStartMs,
                )
                val newPositionMs = toMs.coerceIn(lowerMs, clip.positionEndMs - MIN_CLIP_MS)
                clip.copy(sourceStartMs = clip.sourceMsAt(newPositionMs), positionMs = newPositionMs)
            }

            Edge.End -> {
                val upperMs = minOf(
                    clips.getOrNull(index + 1)?.positionMs ?: maxEndMs,
                    // 원본 끝보다 뒤의 소리는 없다
                    clip.positionMs + (originDurationMs - clip.sourceStartMs),
                )
                val newEndMs = toMs.coerceIn(clip.positionMs + MIN_CLIP_MS, upperMs)
                clip.copy(sourceEndMs = clip.sourceMsAt(newEndMs))
            }
        }
        return replaceClip(index, resized)
    }

    /** 조각을 통째로 타임라인 [toPositionMs]로 옮긴다. 양옆 조각 사이 빈칸 안에서만 움직인다. */
    fun move(index: Int, toPositionMs: Long): AudioEditTimeline {
        if (index !in clips.indices) return this
        val clip = clips[index]

        val lowerMs = clips.getOrNull(index - 1)?.positionEndMs ?: 0L
        val upperMs = (clips.getOrNull(index + 1)?.positionMs ?: maxEndMs) - clip.durationMs
        if (upperMs < lowerMs) return this

        return replaceClip(index, clip.copy(positionMs = toPositionMs.coerceIn(lowerMs, upperMs)))
    }

    /**
     * 조각을 바꾸고, 바뀐 조각이 끝 표시를 넘었으면 끝 표시를 그만큼 늘린다.
     *
     * 끝 표시로 이미 뒤가 잘린 조각은 원래 끝보다 더 나간 만큼만 늘린다. 그래야 다른 조작을 해도
     * 사용자가 잘라 둔 길이가 풀리지 않는다.
     */
    private fun replaceClip(index: Int, clip: AudioEditClip): AudioEditTimeline {
        val overflowMs = clip.positionEndMs - maxOf(clips[index].positionEndMs, endMs)
        return copy(
            clips = clips.toMutableList().apply { set(index, clip) },
            endMs = (endMs + overflowMs.coerceAtLeast(0)).coerceAtMost(maxEndMs),
        )
    }

    fun withEnd(ms: Long): AudioEditTimeline = copy(endMs = ms.coerceIn(MIN_CLIP_MS, maxEndMs))

    /**
     * 0초부터 [endMs]까지를 소리와 무음 조각으로 펼친다.
     *
     * 끝 표시에 걸린 조각은 거기서 자르고, 그 때문에 최소 길이보다 짧아진 조각은 무음으로 바꾼다.
     * 이어진 무음은 하나로 합친다.
     */
    fun toParts(): List<AudioEditPart> {
        val parts = mutableListOf<AudioEditPart>()

        fun addSilence(durationMs: Long) {
            if (durationMs <= 0) return
            val last = parts.lastOrNull()
            if (last is AudioEditPart.Silence) {
                parts[parts.lastIndex] = AudioEditPart.Silence(last.durationMs + durationMs)
            } else {
                parts += AudioEditPart.Silence(durationMs)
            }
        }

        var cursorMs = 0L
        for (clip in clips) {
            if (clip.positionMs >= endMs) break
            val clipEndMs = minOf(clip.positionEndMs, endMs)

            addSilence(clip.positionMs - cursorMs)
            if (clipEndMs - clip.positionMs >= MIN_CLIP_MS) {
                parts += AudioEditPart.Audio(startMs = clip.sourceStartMs, endMs = clip.sourceMsAt(clipEndMs))
            } else {
                addSilence(clipEndMs - clip.positionMs)
            }
            cursorMs = clipEndMs
        }
        addSilence(endMs - cursorMs)

        return parts
    }
}
