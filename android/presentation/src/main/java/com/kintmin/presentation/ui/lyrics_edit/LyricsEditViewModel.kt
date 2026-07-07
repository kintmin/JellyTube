package com.kintmin.presentation.ui.lyrics_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.GetAudioMediaLyricsUseCase
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SerializeLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SplitLyricsByNewlineUseCase
import com.kintmin.presentation.ui.lyrics_edit.LyricsEditUiState.EditRow
import com.kintmin.presentation.ui.lyrics_edit.navigation.LyricsEditScreenRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsEditViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val getAudioMediaLyricsUseCase: GetAudioMediaLyricsUseCase,
    private val getLyricsVariantUseCase: GetLyricsVariantUseCase,
    private val parseLyricsUseCase: ParseLyricsUseCase,
    private val serializeLyricsUseCase: SerializeLyricsUseCase,
    private val splitLyricsByNewlineUseCase: SplitLyricsByNewlineUseCase,
    private val applyLyricsToAudioMediaUseCase: ApplyLyricsToAudioMediaUseCase,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<LyricsEditScreenRoute>().audioMediaId

    private var nextRowId = 0
    private var isLoaded = false
    private var previousLyricFileFullPath: String? = null

    private val _eventFlow = MutableSharedFlow<LyricsEditEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _data = MutableStateFlow(
        LyricsEditUiState(
            title = "",
            durationMs = 0L,
            rows = emptyList(),
            isLoading = true,
            isSaving = false,
            isDirty = false,
        )
    )
    val data = _data.asStateFlow()

    init {
        viewModelScope.launch {
            fetchAudioMediaDetailFlowUseCase(audioMediaId)
                .map { aggregates -> aggregates.firstOrNull()?.audioMedia }
                .distinctUntilChanged()
                .collect { audioMedia ->
                    val title = audioMedia?.name.orEmpty()
                    val durationMs = audioMedia?.audioDuration?.inWholeMilliseconds ?: 0L
                    // 최초 1회만 파일에서 로드해 편집 상태를 시드한다(이후 편집 중 재로드로 덮어쓰지 않음).
                    if (!isLoaded) {
                        isLoaded = true
                        val lyricFileFullPath = audioMedia?.lyricFileFullPath
                        previousLyricFileFullPath = lyricFileFullPath
                        // 번역/음차 변형 파일이 있으면 원본 줄과 인덱스 정렬해 각 행에 붙인다.
                        val translation = lyricFileFullPath?.let {
                            getLyricsVariantUseCase(it, LyricsVariant.TRANSLATION).getOrNull()
                        }
                        val transliteration = lyricFileFullPath?.let {
                            getLyricsVariantUseCase(it, LyricsVariant.TRANSLITERATION).getOrNull()
                        }
                        val rows = if (lyricFileFullPath == null) {
                            emptyList()
                        } else {
                            val rawLyrics = getAudioMediaLyricsUseCase(lyricFileFullPath).getOrNull().orEmpty()
                            val parsed = parseLyricsUseCase(rawLyrics)
                            when {
                                parsed.isEmpty() -> emptyList()
                                // 모든 줄의 시작 시간이 0 이면 타이밍이 없는(plain) 가사로 보고 한 행에 전체를 담는다.
                                parsed.all { (it.timeMs ?: 0L) == 0L } ->
                                    listOf(
                                        newRow(
                                            timeMs = 0L,
                                            text = parsed.joinToString("\n") { it.text },
                                            translation = translation?.joinToString("\n") { it.text }.orEmpty(),
                                            transliteration = transliteration?.joinToString("\n") { it.text }.orEmpty(),
                                            isModified = false,
                                        )
                                    )

                                else -> parsed.mapIndexed { index, line ->
                                    newRow(
                                        timeMs = line.timeMs ?: 0L,
                                        text = line.text,
                                        translation = translation?.getOrNull(index)?.text.orEmpty(),
                                        transliteration = transliteration?.getOrNull(index)?.text.orEmpty(),
                                        isModified = false,
                                    )
                                }
                            }
                        }
                        _data.update {
                            it.copy(
                                title = title,
                                durationMs = durationMs,
                                rows = rows,
                                isLoading = false,
                                hasTranslation = translation != null,
                                hasTransliteration = transliteration != null,
                            )
                        }
                    } else {
                        _data.update { it.copy(title = title, durationMs = durationMs) }
                    }
                }
        }
    }

    fun sendIntent(intent: LyricsEditIntent) {
        when (intent) {
            is LyricsEditIntent.OnChangeTime -> changeTime(intent.rowId, intent.timeMs)
            is LyricsEditIntent.OnChangeText -> changeText(intent.rowId, intent.text)
            is LyricsEditIntent.OnChangeTranslation -> changeTranslation(intent.rowId, intent.text)
            is LyricsEditIntent.OnChangeTransliteration -> changeTransliteration(intent.rowId, intent.text)
            is LyricsEditIntent.OnAddRowBelow -> addRowBelow(intent.rowId)
            is LyricsEditIntent.OnDeleteRow -> deleteRow(intent.rowId)
            LyricsEditIntent.OnSplitByNewline -> splitByNewline()
            LyricsEditIntent.OnClickSave -> save()
        }
    }

    private fun newRow(
        timeMs: Long,
        text: String,
        translation: String = "",
        transliteration: String = "",
        isModified: Boolean,
    ): EditRow =
        EditRow(
            id = nextRowId++,
            timeMs = timeMs,
            text = text,
            translation = translation,
            transliteration = transliteration,
            isModified = isModified,
        )

    private fun changeTime(rowId: Int, timeMs: Long) {
        _data.update { state ->
            state.copy(
                rows = state.rows.map { if (it.id == rowId) it.copy(timeMs = timeMs, isModified = true) else it },
                isDirty = true,
            )
        }
    }

    private fun changeText(rowId: Int, text: String) {
        _data.update { state ->
            state.copy(
                rows = state.rows.map { if (it.id == rowId) it.copy(text = text, isModified = true) else it },
                isDirty = true,
            )
        }
    }

    private fun changeTranslation(rowId: Int, text: String) {
        _data.update { state ->
            state.copy(
                rows = state.rows.map { if (it.id == rowId) it.copy(translation = text, isModified = true) else it },
                isDirty = true,
            )
        }
    }

    private fun changeTransliteration(rowId: Int, text: String) {
        _data.update { state ->
            state.copy(
                rows = state.rows.map { if (it.id == rowId) it.copy(transliteration = text, isModified = true) else it },
                isDirty = true,
            )
        }
    }

    private fun addRowBelow(rowId: Int) {
        _data.update { state ->
            val index = state.rows.indexOfFirst { it.id == rowId }
            if (index < 0) return@update state
            val baseTime = state.rows[index].timeMs
            val newRows = state.rows.toMutableList()
                .apply { add(index + 1, newRow(timeMs = baseTime, text = "", isModified = true)) }
            state.copy(rows = newRows, isDirty = true)
        }
    }

    private fun deleteRow(rowId: Int) {
        _data.update { state -> state.copy(rows = state.rows.filterNot { it.id == rowId }, isDirty = true) }
    }

    private fun splitByNewline() {
        _data.update { state ->
            val oldRows = state.rows
            val split = splitLyricsByNewlineUseCase(
                oldRows.map { LyricsLine(timeMs = it.timeMs, text = it.text) },
                audioEndMs = state.durationMs,
            )
            val newRows = split.map { line ->
                val kept = oldRows.firstOrNull { it.timeMs == line.timeMs && it.text == line.text }
                newRow(
                    timeMs = line.timeMs ?: 0L,
                    text = line.text,
                    translation = kept?.translation.orEmpty(),
                    transliteration = kept?.transliteration.orEmpty(),
                    isModified = kept?.isModified ?: true,
                )
            }
            state.copy(rows = newRows, isDirty = state.isDirty || newRows.size != oldRows.size)
        }
    }

    private fun save() {
        if (_data.value.isSaving) return
        viewModelScope.launch {
            _data.update { it.copy(isSaving = true) }
            val state = _data.value
            val rows = state.rows
            val syncedLyrics = serializeLyricsUseCase(
                rows.map { LyricsLine(timeMs = it.timeMs, text = it.text) },
            )
            // 번역/음차는 원본과 같은 타임코드로 각각 별도 파일에 저장한다(인덱스 정렬 유지).
            val translationLyrics = if (state.hasTranslation) {
                serializeLyricsUseCase(rows.map { LyricsLine(timeMs = it.timeMs, text = it.translation) })
            } else null
            val transliterationLyrics = if (state.hasTransliteration) {
                serializeLyricsUseCase(rows.map { LyricsLine(timeMs = it.timeMs, text = it.transliteration) })
            } else null
            val result = applyLyricsToAudioMediaUseCase(
                audioMediaId = audioMediaId,
                plainLyrics = null,
                syncedLyrics = syncedLyrics,
                translationLyrics = translationLyrics,
                transliterationLyrics = transliterationLyrics,
                previousLyricFileFullPath = previousLyricFileFullPath,
            )
            _data.update { it.copy(isSaving = false) }
            if (result.isSuccess) {
                _eventFlow.emit(LyricsEditEvent.ShowToast("수정사항을 저장했습니다."))
                _eventFlow.emit(LyricsEditEvent.NavigateToBack)
            } else {
                _eventFlow.emit(LyricsEditEvent.ShowToast("저장에 실패했습니다."))
            }
        }
    }
}
