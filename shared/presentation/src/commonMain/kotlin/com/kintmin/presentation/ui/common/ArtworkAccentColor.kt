package com.kintmin.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.kintmin.presentation.platform.extractDominantColor
import com.kintmin.presentation.theme.liftedForAccent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 동시에 들고 있는 곡 수보다 넉넉하면 된다 — 재생 중인 곡, 목록에 보이는 곡, 직전 곡. */
private const val CACHE_LIMIT = 16

private val cacheLock = Mutex()

// 삽입 순서를 보존해 넘칠 때 가장 오래된 것을 버린다.
private val cache = LinkedHashMap<String, Int>()

/**
 * 대표색 ARGB. 읽을 수 없으면 null.
 *
 * 추출은 파일을 디코딩해야 해서, 재생바·목록·상세가 같은 그림을 각각 읽으면 화면을 옮길 때마다
 * 멈칫한다. 경로로 기억해 한 번만 읽는다.
 */
suspend fun artworkDominantColorArgb(imageFileFullPath: String): Int? {
    cacheLock.withLock { cache[imageFileFullPath] }?.let { return it }

    val extracted = extractDominantColor(imageFileFullPath) ?: return null

    cacheLock.withLock {
        cache[imageFileFullPath] = extracted
        if (cache.size > CACHE_LIMIT) {
            cache.remove(cache.keys.first())
        }
    }
    return extracted
}

/**
 * 대표색을 그래픽 대비 바닥값까지 끌어올린 강조색. 아직 읽는 중이거나 읽을 수 없으면 null 이라,
 * 호출부가 테마 강조색으로 떨어진다.
 */
@Composable
fun rememberArtworkAccentColor(imageFileFullPath: String?): Color? {
    var accent by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(imageFileFullPath) {
        accent = if (imageFileFullPath.isNullOrBlank()) {
            null
        } else {
            artworkDominantColorArgb(imageFileFullPath)?.let { Color(it).liftedForAccent() }
        }
    }
    return accent
}
