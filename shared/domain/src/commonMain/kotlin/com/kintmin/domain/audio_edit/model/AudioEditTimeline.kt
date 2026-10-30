package com.kintmin.domain.audio_edit.model

/**
 * 편집 화면의 타임라인. 원본에서 떼어 온 조각(clip)들의 배치와 결과물의 끝 위치를 들고 있다.
 *
 * - 시각은 모두 타임라인 기준이다. 조각마다 원본 구간과 타임라인 위치를 따로 가진다([AudioEditClip]).
 * - 조각이 없는 곳은 빈칸이고, 저장할 때 무음으로 들어간다.
 * - [endMs]가 결과물의 끝이다. 마지막 조각보다 뒤면 무음이 붙고, 앞이면 그 뒤 소리가 잘린다.
 *   조각을 옮기거나 늘려 끝을 넘기면 넘긴 만큼 끝이 따라 늘어난다. 줄어드는 쪽으로는 따라가지 않는다.
 * - 조각끼리는 겹칠 수 없고 0초 앞으로 갈 수 없다.
 * - 병합한 조각들은 묶음 하나로 다룬다. 같이 움직이고, 묶음 양 끝 경계만 끌 수 있으며,
 *   선택·삭제도 묶음 단위다. 묶음 안에서 자르면 그 자리에서 묶음이 둘로 나뉘고,
 *   병합했던 이음매([seamPositionsMs])에서 자르면 병합이 풀려 원래 두 조각으로 돌아간다.
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

    /** 병합된 조각 묶음들. 각 묶음은 [clips]의 index 범위이며, 병합하지 않은 조각은 혼자 한 묶음이다. */
    val groupRanges: List<IntRange>
        get() = clips.indices
            .filter { !clips[it].isLinkedToPrevious }
            .map { first -> first..groupLastIndex(first) }

    /** 원본에서 이어지지 않은 조각을 병합한 이음매 위치들. 화면에 점선으로 보이고, 여기서 자르면 병합이 풀린다. */
    val seamPositionsMs: List<Long>
        get() = clips.filter { it.isLinkedToPrevious }.map { it.positionMs }

    /** [index] 조각이 속한 묶음의 index 범위. */
    fun groupRangeOf(index: Int): IntRange {
        var first = index
        while (first > 0 && clips[first].isLinkedToPrevious) first--
        return first..groupLastIndex(index)
    }

    private fun groupLastIndex(index: Int): Int {
        var last = index
        while (clips.getOrNull(last + 1)?.isLinkedToPrevious == true) last++
        return last
    }

    /** [atMs]에서 나눴을 때 양쪽이 모두 최소 길이 이상인 조각의 index. */
    private fun splittableClipIndex(atMs: Long): Int? =
        clips.indexOfFirst {
            atMs - it.positionMs >= MIN_CLIP_MS && it.positionEndMs - atMs >= MIN_CLIP_MS
        }.takeIf { it >= 0 }

    /** [atMs]가 병합 이음매면 그 뒤 조각의 index. */
    private fun seamClipIndex(atMs: Long): Int? =
        clips.indexOfFirst { it.isLinkedToPrevious && it.positionMs == atMs }.takeIf { it >= 0 }

    fun canSplit(atMs: Long): Boolean = splittableClipIndex(atMs) != null || seamClipIndex(atMs) != null

    /**
     * [atMs]에서 조각을 둘로 나눈다. 병합 이음매에서 자르면 조각은 그대로 두고 병합만 푼다.
     */
    fun split(atMs: Long): AudioEditTimeline {
        seamClipIndex(atMs)?.let { seamIndex ->
            return copy(
                clips = clips.toMutableList().apply {
                    set(seamIndex, clips[seamIndex].copy(isLinkedToPrevious = false))
                }
            )
        }
        val index = splittableClipIndex(atMs) ?: return this
        val clip = clips[index]
        val splitSourceMs = clip.sourceMsAt(atMs)
        return copy(
            clips = clips.toMutableList().apply {
                set(index, clip.copy(sourceEndMs = splitSourceMs))
                // 뒤쪽은 앞과 끊어 새 묶음을 시작한다. 그 뒤에 붙어 있던 조각들은 뒤쪽과 같이 간다.
                add(
                    index + 1,
                    clip.copy(sourceStartMs = splitSourceMs, positionMs = atMs, isLinkedToPrevious = false),
                )
            }
        )
    }

    /** [atMs]에서 맞닿아 있고 아직 병합되지 않은 두 조각 중 앞 조각의 index. */
    private fun mergeableClipIndex(atMs: Long): Int? =
        (0 until clips.lastIndex).firstOrNull { index ->
            val next = clips[index + 1]
            clips[index].positionEndMs == atMs && next.positionMs == atMs && !next.isLinkedToPrevious
        }

    /** 처음(0초)과 마지막 조각의 끝, 빈칸과 맞닿은 경계에서는 붙일 조각이 없어 병합할 수 없다. */
    fun canMerge(atMs: Long): Boolean = mergeableClipIndex(atMs) != null

    /**
     * [atMs]에서 맞닿은 두 조각을 한 덩이로 병합한다.
     *
     * 원본에서 이어진 구간이면(자른 것을 다시 붙이는 경우) 한 조각으로 합치고,
     * 아니면(옮겨서 다른 부분 옆에 붙인 경우) 묶음으로 엮어 같이 움직이게 한다.
     */
    fun merge(atMs: Long): AudioEditTimeline {
        val index = mergeableClipIndex(atMs) ?: return this
        val clip = clips[index]
        val next = clips[index + 1]
        return copy(
            clips = clips.toMutableList().apply {
                if (clip.sourceEndMs == next.sourceStartMs) {
                    set(index, clip.copy(sourceEndMs = next.sourceEndMs))
                    removeAt(index + 1)
                } else {
                    set(index + 1, next.copy(isLinkedToPrevious = true))
                }
            }
        )
    }

    /** [index] 조각이 속한 묶음을 통째로 지운다. */
    fun delete(index: Int): AudioEditTimeline {
        if (index !in clips.indices) return this
        val group = groupRangeOf(index)
        return copy(clips = clips.filterIndexed { clipIndex, _ -> clipIndex !in group })
    }

    /**
     * 조각 한쪽 끝을 타임라인 [toMs]로 옮긴다. 바깥으로 끌면 원본 소리가 되살아나고, 안쪽으로 끌면 줄어든다.
     *
     * 이웃 조각과 겹치거나, 0초 앞으로 가거나, 원본에 없는 소리를 끌어오거나,
     * 최소 길이보다 짧아지거나, 끝 표시 상한을 넘지 않게 맞춘다.
     * 묶음 안쪽 경계는 한 덩이의 일부라 끌 수 없고, 묶음 양 끝만 끌 수 있다.
     */
    fun resizeEdge(index: Int, edge: Edge, toMs: Long): AudioEditTimeline {
        if (index !in clips.indices) return this
        val clip = clips[index]
        // 최소 길이보다 짧은 원본이면 움직일 여지가 없다
        if (clip.durationMs < MIN_CLIP_MS) return this
        val isInnerEdge = when (edge) {
            Edge.Start -> clip.isLinkedToPrevious
            Edge.End -> clips.getOrNull(index + 1)?.isLinkedToPrevious == true
        }
        if (isInnerEdge) return this

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
        return replaceClips(firstIndex = index, replaced = listOf(resized))
    }

    /**
     * [index] 조각이 타임라인 [toPositionMs]에 오도록 그 조각이 속한 묶음을 통째로 옮긴다.
     * 묶음 양옆 조각 사이 빈칸 안에서만 움직인다.
     */
    fun move(index: Int, toPositionMs: Long): AudioEditTimeline {
        if (index !in clips.indices) return this
        val group = groupRangeOf(index)
        val groupStartMs = clips[group.first].positionMs
        val groupDurationMs = clips[group.last].positionEndMs - groupStartMs

        val lowerMs = clips.getOrNull(group.first - 1)?.positionEndMs ?: 0L
        val upperMs = (clips.getOrNull(group.last + 1)?.positionMs ?: maxEndMs) - groupDurationMs
        if (upperMs < lowerMs) return this

        val newGroupStartMs = (groupStartMs + (toPositionMs - clips[index].positionMs)).coerceIn(lowerMs, upperMs)
        val shiftMs = newGroupStartMs - groupStartMs
        return replaceClips(
            firstIndex = group.first,
            replaced = group.map { clipIndex -> clips[clipIndex].let { it.copy(positionMs = it.positionMs + shiftMs) } },
        )
    }

    /**
     * [firstIndex]부터 연속된 조각들을 바꾸고, 바뀐 조각이 끝 표시를 넘었으면 끝 표시를 그만큼 늘린다.
     *
     * 끝 표시로 이미 뒤가 잘린 조각은 원래 끝보다 더 나간 만큼만 늘린다. 그래야 다른 조작을 해도
     * 사용자가 잘라 둔 길이가 풀리지 않는다.
     */
    private fun replaceClips(firstIndex: Int, replaced: List<AudioEditClip>): AudioEditTimeline {
        val lastIndex = firstIndex + replaced.lastIndex
        val overflowMs = replaced.last().positionEndMs - maxOf(clips[lastIndex].positionEndMs, endMs)
        return copy(
            clips = clips.toMutableList().apply {
                replaced.forEachIndexed { offset, clip -> set(firstIndex + offset, clip) }
            },
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
