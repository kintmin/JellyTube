package com.kintmin.data.python_bridge

/** [unsupportedReason] 가 있으면 앱이 조각을 직접 받지 않는다. 사유는 fallback 로그에 그대로 실린다. */
data class HlsMediaPlaylist(
    val segmentUrls: List<String>,
    val unsupportedReason: String?,
) {
    companion object {

        /** 앱이 직접 받는 HLS 는 이 태그만 쓰는 평문 VOD 미디어 재생목록뿐이다. */
        val SUPPORTED_TAGS: Set<String> = setOf(
            "#EXTM3U",
            "#EXT-X-VERSION",
            "#EXT-X-PLAYLIST-TYPE",
            "#EXT-X-TARGETDURATION",
            "#EXT-X-MEDIA-SEQUENCE",
            "#EXT-X-INDEPENDENT-SEGMENTS",
            "#EXTINF",
            "#EXT-X-ENDLIST",
        )

        fun parse(text: String, baseUrl: String): HlsMediaPlaylist {
            val segmentUrls = mutableListOf<String>()
            val unknownTags = mutableListOf<String>()
            var hasEndList = false

            for (rawLine in text.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                if (!line.startsWith("#")) {
                    segmentUrls += resolve(baseUrl, line)
                    continue
                }
                // `#EXT` 로 시작하지 않는 줄은 주석이다(RFC 8216 4.1).
                if (!line.startsWith("#EXT")) continue

                val tag = line.substringBefore(':')
                val value = line.substringAfter(':', "").trim()
                when {
                    tag == "#EXT-X-STREAM-INF" -> return unsupported("master_playlist")
                    tag !in SUPPORTED_TAGS -> if (tag.drop(1) !in unknownTags) unknownTags += tag.drop(1)
                    tag == "#EXT-X-ENDLIST" -> hasEndList = true
                    tag == "#EXT-X-PLAYLIST-TYPE" && value != "VOD" -> return unsupported("playlist_type:$value")
                }
            }

            return when {
                unknownTags.isNotEmpty() -> unsupported("unknown_tag:" + unknownTags.joinToString(","))
                !hasEndList -> unsupported("no_endlist")
                segmentUrls.isEmpty() -> unsupported("no_segments")
                else -> HlsMediaPlaylist(segmentUrls, unsupportedReason = null)
            }
        }

        private fun unsupported(reason: String) = HlsMediaPlaylist(emptyList(), reason)

        private val SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

        /** RFC 3986 5.2 상대 참조 풀이. 조각 주소는 리다이렉트된 뒤의 재생목록 주소를 기준으로 푼다. */
        internal fun resolve(baseUrl: String, reference: String): String {
            if (SCHEME.containsMatchIn(reference)) return reference

            val scheme = baseUrl.substringBefore(':')
            if (reference.startsWith("//")) return "$scheme:$reference"

            val hierarchy = baseUrl.substringAfter("://")
            val authorityEnd = hierarchy.indexOfAny(charArrayOf('/', '?', '#')).let { if (it < 0) hierarchy.length else it }
            val authority = hierarchy.substring(0, authorityEnd)
            val basePathAndQuery = hierarchy.substring(authorityEnd).substringBefore('#')
            val basePath = basePathAndQuery.substringBefore('?')
            val prefix = "$scheme://$authority"

            if (reference.isEmpty()) return prefix + basePathAndQuery
            if (reference.startsWith("?")) return prefix + basePath + reference

            val referencePath = reference.substringBefore('?').substringBefore('#')
            val referenceRest = reference.substring(referencePath.length)
            val mergedPath = when {
                referencePath.startsWith("/") -> referencePath
                basePath.isEmpty() -> "/$referencePath"
                else -> basePath.substringBeforeLast('/') + "/" + referencePath
            }
            return prefix + removeDotSegments(mergedPath) + referenceRest
        }

        private fun removeDotSegments(path: String): String {
            val output = ArrayDeque<String>()
            val segments = path.split('/').drop(1)
            segments.forEachIndexed { index, segment ->
                when (segment) {
                    "." -> if (index == segments.lastIndex) output.addLast("")
                    ".." -> {
                        output.removeLastOrNull()
                        if (index == segments.lastIndex) output.addLast("")
                    }
                    else -> output.addLast(segment)
                }
            }
            return "/" + output.joinToString("/")
        }
    }
}
