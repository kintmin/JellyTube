package com.kintmin.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class MlKitLyricsTranslator : LyricsTranslator {

    override suspend fun translateToKorean(
        text: String,
        sourceLanguage: String,
    ): Result<String> = runCatching {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage)
            ?: throw IllegalArgumentException("지원하지 않는 언어 코드: $sourceLanguage")

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        val translator = Translation.getClient(options)
        translator.use { translator ->
            // 최초 1회 온디바이스 모델 다운로드 (네트워크 필요). 이후엔 캐시된 모델로 오프라인 동작.
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()

            // 줄 단위로 번역해 가사 라인 구조를 보존한다.
            text.split("\n").map { line ->
                if (line.isBlank()) {
                    line
                } else {
                    translator.translate(line).await()
                }
            }.joinToString("\n")
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { error -> cont.resumeWithException(error) }
}
