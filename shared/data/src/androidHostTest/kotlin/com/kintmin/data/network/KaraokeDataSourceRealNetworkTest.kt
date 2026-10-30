package com.kintmin.data.network

import com.kintmin.data.network.dataSourceImpl.KaraokeDataSourceImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class KaraokeDataSourceRealNetworkTest {

    @Test
    fun searchBySongTitle_returnsTjNumber_forKnownSongs() = runTest {
        val client = createHttpClient()
        val dataSource = KaraokeDataSourceImpl(client)

        val titles = listOf(
            "벚꽃엔딩",
        )

        for (title in titles) {
            val result = dataSource.searchBySongTitle(title)
            assertTrue("검색 실패: $title -> ${result.exceptionOrNull()}", result.isSuccess)

            val songs = result.getOrThrow()
            assertTrue("결과 없음: $title", songs.isNotEmpty())
            assertTrue("brand가 tj가 아님: $title", songs.all { it.brand == "tj" })
            assertTrue("TJ 번호 비어있음: $title", songs.all { it.no.isNotBlank() })
        }
    }

    @Test
    fun searchBySongTitle_returnsEmpty_forNonexistentSong() = runTest {
        val client = createHttpClient()
        val dataSource = KaraokeDataSourceImpl(client)

        val result = dataSource.searchBySongTitle("zzxxqqnonexist999")
        assertTrue("검색 실패: ${result.exceptionOrNull()}", result.isSuccess)
        assertTrue("빈 결과여야 함", result.getOrThrow().isEmpty())
    }
}
