package com.kintmin.domain.playlist_download

object PlaylistMergeOrder {

    fun of(current: List<Int>, youtubeOrder: List<Int>): List<Int> {
        val result = current.toMutableList()
        youtubeOrder.forEachIndexed { index, id ->
            if (id in result) return@forEachIndexed
            // 앞 곡은 이미 결과에 있다 — 있던 곡이거나 앞에서 끼운 곡이다.
            if (index > 0) {
                result.add(result.indexOf(youtubeOrder[index - 1]) + 1, id)
                return@forEachIndexed
            }
            val after = youtubeOrder.drop(1).firstOrNull { it in result }
            if (after != null) result.add(result.indexOf(after), id) else result.add(id)
        }
        return result
    }
}
