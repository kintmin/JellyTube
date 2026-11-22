package com.kintmin.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class MlKitLyricsTranslationEngine : LyricsTranslationEngine {

    override suspend fun detectLanguages(lines: List<String>): List<String?> =
        LanguageIdentification.getClient().use { identifier ->
            lines.map { line ->
                identifier.identifyLanguage(line).await()
                    .takeUnless { it == LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG }
            }
        }

    override suspend fun translateToKorean(lines: List<String>, sourceLanguage: String): List<String> {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage) ?: return lines

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        return Translation.getClient(options).use { translator ->
            // 최초 1회 온디바이스 모델 다운로드 (네트워크 필요). 이후엔 캐시된 모델로 오프라인 동작.
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            lines.map { line -> translator.translate(line).await() }
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { error -> cont.resumeWithException(error) }
}
