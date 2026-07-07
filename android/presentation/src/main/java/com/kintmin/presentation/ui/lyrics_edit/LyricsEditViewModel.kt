package com.kintmin.presentation.ui.lyrics_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.GetAudioMediaLyricsUseCase
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
    private val parseLyricsUseCase: ParseLyricsUseCase,
    private val serializeLyricsUseCase: SerializeLyricsUseCase,
    private val splitLyricsByNewlineUseCase: SplitLyricsByNewlineUseCase,
    private val applyLyricsToAudioMediaUseCase: ApplyLyricsToAudioMediaUseCase,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<LyricsEditScreenRoute>().audioMediaId

    private var nextRowId = 0
    private var isLoaded = false

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
                        val rows = if (lyricFileFullPath == null) {
                            emptyList()
                        } else {
                            val rawLyrics = getAudioMediaLyricsUseCase(lyricFileFullPath).getOrNull().orEmpty()
                            val parsed = parseLyricsUseCase(rawLyrics)
                            when {
                                parsed.isEmpty() -> emptyList()
                                // 모든 줄의 시작 시간이 0 이면 타이밍이 없는(plain) 가사로 보고 한 행에 전체를 담는다.
                                parsed.all { (it.timeMs ?: 0L) == 0L } ->
                                    listOf(newRow(0L, parsed.joinToString("\n") { it.text }, false))

                                else -> parsed.map { line -> newRow(line.timeMs ?: 0L, line.text, false) }
                            }
                        }
                        _data.update {
                            it.copy(title = title, durationMs = durationMs, rows = rows, isLoading = false)
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
            is LyricsEditIntent.OnAddRowBelow -> addRowBelow(intent.rowId)
            is LyricsEditIntent.OnDeleteRow -> deleteRow(intent.rowId)
            is LyricsEditIntent.OnReorder -> reorder(intent.orderedIds)
            LyricsEditIntent.OnSplitByNewline -> splitByNewline()
            LyricsEditIntent.OnClickSave -> save()
        }
    }

    private fun newRow(timeMs: Long, text: String, isModified: Boolean): EditRow =
        EditRow(id = nextRowId++, timeMs = timeMs, text = text, isModified = isModified)

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

    private fun addRowBelow(rowId: Int) {
        _data.update { state ->
            val index = state.rows.indexOfFirst { it.id == rowId }
            if (index < 0) return@update state
            val baseTime = state.rows[index].timeMs
            val newRows = state.rows.toMutableList().apply { add(index + 1, newRow(baseTime, "", true)) }
            state.copy(rows = newRows, isDirty = true)
        }
    }

    private fun deleteRow(rowId: Int) {
        _data.update { state -> state.copy(rows = state.rows.filterNot { it.id == rowId }, isDirty = true) }
    }

    private fun reorder(orderedIds: List<Int>) {
        _data.update { state ->
            val byId = state.rows.associateBy { it.id }
            state.copy(rows = orderedIds.mapNotNull { byId[it] }, isDirty = true)
        }
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
                newRow(line.timeMs ?: 0L, line.text, kept?.isModified ?: true)
            }
            state.copy(rows = newRows, isDirty = state.isDirty || newRows.size != oldRows.size)
        }
    }

    private fun save() {
        if (_data.value.isSaving) return
        viewModelScope.launch {
            _data.update { it.copy(isSaving = true) }
            val syncedLyrics = serializeLyricsUseCase(
                _data.value.rows.map { LyricsLine(timeMs = it.timeMs, text = it.text) },
            )
            val result = applyLyricsToAudioMediaUseCase(
                audioMediaId = audioMediaId,
                plainLyrics = null,
                syncedLyrics = syncedLyrics,
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
