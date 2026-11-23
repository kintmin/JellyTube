package com.kintmin.jellytube.python_bridge_impl

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kintmin.data.python_bridge.LyricsTransliterator
import com.kintmin.domain.common.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsTransliteratorImpl(
    private val context: Context,
) : LyricsTransliterator {

    override suspend fun transliterateToKorean(text: String): Result<String> = suspendRunCatching {
        withContext(Dispatchers.IO) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val module = Python.getInstance().getModule(FILE_NAME)
            // Python 측 예외는 PyException 으로 전파되어 runCatching 이 Result.failure 로 감싼다.
            module.callAttr(METHOD_TRANSLITERATE, text).toString()
        }
    }

    private companion object {
        const val FILE_NAME = "transliterate_lyrics"
        const val METHOD_TRANSLITERATE = "transliterate_to_korean"
    }
}
