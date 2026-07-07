package com.kintmin.data.repository_impl

import com.kintmin.data.network.dataSource.LyricsDataSource
import com.kintmin.data.network.model.LyricsSearchResultDto
import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.domain.lyrics.repository.LyricsRepository

internal class LyricsRepositoryImpl(
    private val lyricsDataSource: LyricsDataSource,
) : LyricsRepository {

    override suspend fun search(query: String): Result<List<LyricsSearchResult>> =
        lyricsDataSource.search(query).map { list -> list.map { it.toDomain() } }
}

private fun LyricsSearchResultDto.toDomain() = LyricsSearchResult(
    id = id,
    trackName = trackName,
    artistName = artistName,
    albumName = albumName,
    duration = duration,
    plainLyrics = plainLyrics,
    syncedLyrics = syncedLyrics,
)
