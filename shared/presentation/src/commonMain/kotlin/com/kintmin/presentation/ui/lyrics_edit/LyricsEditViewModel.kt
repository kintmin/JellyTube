package com.kintmin.presentation.ui.lyrics_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.EditedLyricsRow
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SaveEditedLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SplitLyricsByNewlineUseCase
import com.kintmin.presentation.ui.lyrics_edit.LyricsEditUiState.EditRow
import com.kintmin.presentation.ui.lyrics_edit.navigation.LyricsEditScreenRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsEditViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val getParsedLyricsUseCase: GetParsedLyricsUseCase,
    private val getLyricsVariantUseCase: GetLyricsVariantUseCase,
    private val splitLyricsByNewlineUseCase: SplitLyricsByNewlineUseCase,
    private val saveEditedLyricsUseCase: SaveEditedLyricsUseCase,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<LyricsEditScreenRoute>().audioMediaId

    private var nextRowId = 0
    private var isLoaded = false
    private var previousLyricFileFullPath: String? = null
    private var lastDeletedRow: Pair<Int, EditRow>? = null

    private val eventChannel = Channel<LyricsEditEvent>(Channel.BUFFERED)
    val eventFlow: Flow<LyricsEditEvent> = eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(
        LyricsEditUiState(
            title = "",
            durationMs = 0L,
            rows = emptyList(),
            isLoading = true,
            isSaving = false,
            isDirty = false,
        )
    )
    val uiState: StateFlow<LyricsEditUiState> = _uiState.asStateFlow()

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
                        val loadedRows = if (lyricFileFullPath == null) {
                            emptyList()
                        } else {
                            val parsed = getParsedLyricsUseCase(lyricFileFullPath)
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
                        // 가사 파일이 없거나 비어 있으면 바로 입력할 수 있도록 빈 편집 행 1개를 시드한다.
                        val rows = loadedRows.ifEmpty {
                            listOf(newRow(timeMs = 0L, text = "", isModified = false))
                        }
                        _uiState.update {
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
                        _uiState.update { it.copy(title = title, durationMs = durationMs) }
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
            LyricsEditIntent.OnUndoDeleteRow -> undoDeleteRow()
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
        _uiState.update { state ->
            state.copy(
                // LRC 는 시간 순이어야 한다.
                rows = state.rows
                    .map { if (it.id == rowId) it.copy(timeMs = timeMs, isModified = true) else it }
                    .sortedBy { it.timeMs },
                isDirty = true,
            )
        }
    }

    private fun changeText(rowId: Int, text: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { if (it.id == rowId) it.copy(text = text, isModified = true) else it },
                isDirty = true,
            )
        }
    }

    private fun changeTranslation(rowId: Int, text: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { if (it.id == rowId) it.copy(translation = text, isModified = true) else it },
                isDirty = true,
            )
        }
    }

    private fun changeTransliteration(rowId: Int, text: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { if (it.id == rowId) it.copy(transliteration = text, isModified = true) else it },
                isDirty = true,
            )
        }
    }

    private fun addRowBelow(rowId: Int) {
        _uiState.update { state ->
            val index = state.rows.indexOfFirst { it.id == rowId }
            if (index < 0) return@update state
            val baseTime = state.rows[index].timeMs
            val newRows = state.rows.toMutableList()
                .apply { add(index + 1, newRow(timeMs = baseTime, text = "", isModified = true)) }
            state.copy(rows = newRows, isDirty = true)
        }
    }

    private fun deleteRow(rowId: Int) {
        val index = _uiState.value.rows.indexOfFirst { it.id == rowId }
        if (index < 0) return
        lastDeletedRow = index to _uiState.value.rows[index]
        _uiState.update { state -> state.copy(rows = state.rows.filterNot { it.id == rowId }, isDirty = true) }
        viewModelScope.launch { eventChannel.send(LyricsEditEvent.ShowUndoDeleteRow) }
    }

    private fun undoDeleteRow() {
        val (index, row) = lastDeletedRow ?: return
        lastDeletedRow = null
        _uiState.update { state ->
            val rows = state.rows.toMutableList().apply { add(index.coerceAtMost(size), row) }
            state.copy(rows = rows)
        }
    }

    private fun splitByNewline() {
        _uiState.update { state ->
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
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val rows = state.rows
            val result = saveEditedLyricsUseCase(
                audioMediaId = audioMediaId,
                rows = rows.map {
                    EditedLyricsRow(timeMs = it.timeMs, text = it.text, translation = it.translation, transliteration = it.transliteration)
                },
                hasTranslation = state.hasTranslation,
                hasTransliteration = state.hasTransliteration,
                previousLyricFileFullPath = previousLyricFileFullPath,
            )
            _uiState.update { it.copy(isSaving = false) }
            if (result.isSuccess) {
                eventChannel.send(LyricsEditEvent.ShowToast("가사를 저장했습니다."))
                eventChannel.send(LyricsEditEvent.NavigateToBack)
            } else {
                eventChannel.send(LyricsEditEvent.ShowToast("가사를 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요."))
            }
        }
    }
}
