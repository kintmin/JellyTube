package com.kintmin.domain.playlist_download

object PlaylistMergeOrder {

    fun of(current: List<Int>, youtubeOrder: List<Int>): List<Int> {
        val result = current.toMutableList()
        youtubeOrder.forEachIndexed { index, id ->
            if (id in result) return@forEachIndexed
            val before = youtubeOrder.subList(0, index).lastOrNull { it in result }
            val after = youtubeOrder.subList(index + 1, youtubeOrder.size).firstOrNull { it in result }
            when {
                before != null -> result.add(result.indexOf(before) + 1, id)
                after != null -> result.add(result.indexOf(after), id)
                else -> result.add(id)
            }
        }
        return result
    }
}
