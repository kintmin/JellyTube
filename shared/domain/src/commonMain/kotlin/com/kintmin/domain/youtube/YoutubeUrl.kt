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
        "youtu.be",
    )
    private val DOWNLOADABLE_FIRST_PATH_SEGMENTS = setOf("watch", "shorts")

    fun isDownloadable(url: String): Boolean {
        val host = hostOf(url)?.lowercase() ?: return false
        val firstPathSegment = firstPathSegmentOf(url) ?: return false

        return host in DOWNLOADABLE_HOSTS && firstPathSegment in DOWNLOADABLE_FIRST_PATH_SEGMENTS
    }

    /** 같은 영상을 가리키는 주소는 `si`·`t` 같은 파라미터나 호스트가 달라도 같은 값을 낸다. 영상 주소가 아니면 null. */
    fun videoIdOf(url: String): String? {
        val host = hostOf(url)?.lowercase() ?: return null
        if (host !in DOWNLOADABLE_HOSTS) return null
        val pathSegments = pathOf(url)?.split('/').orEmpty()

        val videoId = when {
            host == "youtu.be" -> pathSegments.firstOrNull()
            pathSegments.firstOrNull() == "watch" -> firstQueryValueOf(url, key = "v")
            pathSegments.firstOrNull() == "shorts" -> pathSegments.getOrNull(1)
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

    private fun firstPathSegmentOf(url: String): String? =
        pathOf(url)?.substringBefore('/')?.ifEmpty { null }

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

    /** 같은 키가 여러 번이면 첫 번째가 이긴다(`UrlParts` 와 같다). */
    private fun firstQueryValueOf(url: String, key: String): String? {
        val query = url.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
        return query.split('&')
            .firstOrNull { it.substringBefore('=') == key }
            ?.substringAfter('=', missingDelimiterValue = "")
    }
}
