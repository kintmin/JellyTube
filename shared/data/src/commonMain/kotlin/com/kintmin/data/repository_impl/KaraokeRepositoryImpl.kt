package com.kintmin.data.repository_impl

import com.kintmin.data.network.dataSource.KaraokeDataSource
import com.kintmin.data.network.model.KaraokeSongDto
import com.kintmin.domain.karaoke.model.KaraokeSong
import com.kintmin.domain.karaoke.repository.KaraokeRepository

internal class KaraokeRepositoryImpl(
    private val karaokeDataSource: KaraokeDataSource,
) : KaraokeRepository {

    override suspend fun searchBySongTitle(title: String): Result<List<KaraokeSong>> =
        karaokeDataSource.searchBySongTitle(title).map { list -> list.map { it.toDomain() } }
}

private fun KaraokeSongDto.toDomain() = KaraokeSong(
    number = no,
    title = title.orEmpty(),
    singer = singer.orEmpty(),
)
