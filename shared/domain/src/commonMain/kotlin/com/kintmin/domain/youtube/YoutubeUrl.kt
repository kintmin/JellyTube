package com.kintmin.domain.youtube

/**
 * 다운로드 대상이 되는 유튜브 영상 URL 인지 판정한다. 플랫폼 URI 파서를 쓰지 않는 것은
 * 관용적이라 같은 주소를 플랫폼마다 다르게 읽을 수 있어서다.
 */
object YoutubeUrl {

    private val DOWNLOADABLE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
    )

    /** yt-dlp 는 영상 ID 를 뽑을 수 있는 주소면 받는다. 같은 기준으로 가른다. */
    fun isDownloadable(url: String): Boolean = videoIdOf(url) != null

    /** 같은 영상을 가리키는 주소는 `si`·`t` 같은 파라미터나 호스트가 달라도 같은 값을 낸다. 영상 주소가 아니면 null. */
    fun videoIdOf(url: String): String? {
        val host = hostOf(url)?.lowercase() ?: return null
        if (host !in DOWNLOADABLE_HOSTS) return null
        val pathSegments = pathOf(url)?.split('/').orEmpty()

        val videoId = when {
            host == "youtu.be" -> pathSegments.firstOrNull()
            pathSegments.firstOrNull() == "watch" -> firstQueryValueOf(url, key = "v")
            // 라이브(종료된 것 포함)는 공유하면 `/live/{id}` 이고, 모바일 웹은 이 경로를 watch 로 바꾸지 않는다.
            pathSegments.firstOrNull() == "shorts" || pathSegments.firstOrNull() == "live" -> pathSegments.getOrNull(1)
            else -> null
        }
        return videoId?.ifEmpty { null }
    }

    /** 포트·userinfo 는 떼어낸다. */
    private fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val hostWithPort = authority.substringAfterLast('@')
        val host = hostWithPort.substringBefore(':')

        return host.ifEmpty { null }
    }

    /** 앞의 `/` 를 뗀 경로. 쿼리·프래그먼트는 뺀다. */
    private fun pathOf(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authorityEnd = afterScheme.indexOf('/')
        if (authorityEnd == -1) return null

        return afterScheme.substring(authorityEnd + 1)
            .substringBefore('?')
            .substringBefore('#')
    }

    /** 같은 키가 여러 번이면 첫 번째가 이긴다. */
    private fun firstQueryValueOf(url: String, key: String): String? {
        val query = url.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
        return query.split('&')
            .firstOrNull { it.substringBefore('=') == key }
            ?.substringAfter('=', missingDelimiterValue = "")
    }
}
