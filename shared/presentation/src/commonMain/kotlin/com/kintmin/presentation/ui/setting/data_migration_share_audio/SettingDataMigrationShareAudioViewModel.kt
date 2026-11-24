package com.kintmin.presentation.ui.setting.data_migration_share_audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.data_migration.usecase.FetchShareableAudioMediaListFlowUseCase
import com.kintmin.presentation.extension.matchKorean
import com.kintmin.presentation.ui.setting.data_migration_share_audio.list.toSettingDataMigrationShareAudioListItemUiState
import com.kintmin.presentation.util.Debounce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingDataMigrationShareAudioViewModel constructor(
    fetchShareableAudioMediaListFlowUseCase: FetchShareableAudioMediaListFlowUseCase,
) : ViewModel() {

    private val checkedAudioMediaIdList = MutableStateFlow(listOf<Int>())
    private val searchText = MutableStateFlow("")

    private val changeSearchTextDebounce = Debounce(200L)

    private val audioMediaListFlow: StateFlow<List<AudioMedia>> = fetchShareableAudioMediaListFlowUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SettingDataMigrationShareAudioUiState> = combine(
        audioMediaListFlow,
        checkedAudioMediaIdList,
        searchText,
    ) { audioMediaList, checkedIds, keyword ->
        SettingDataMigrationShareAudioUiState(
            audioMediaList = audioMediaList
                .filter { it.name.matchKorean(keyword) }
                .map { it.toSettingDataMigrationShareAudioListItemUiState(isChecked = it.id in checkedIds) },
            // 목록에서 사라진 곡이 선택에 남아 있으면 보내는 수가 실제와 어긋난다.
            checkedAudioMediaIdList = checkedIds.filter { id -> audioMediaList.any { it.id == id } },
            isAllChecked = audioMediaList.isNotEmpty() && checkedIds.size == audioMediaList.size,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingDataMigrationShareAudioUiState(),
    )

    fun sendIntent(intent: SettingDataMigrationShareAudioIntent) {
        when (intent) {
            is SettingDataMigrationShareAudioIntent.OnChangeSearchText -> changeSearchText(intent.searchText)
            is SettingDataMigrationShareAudioIntent.OnClickAudioItem -> checkItem(intent.audioMediaId)
            SettingDataMigrationShareAudioIntent.OnClickSelectAll -> selectAll()
            SettingDataMigrationShareAudioIntent.OnClickClearCheckedList ->
                checkedAudioMediaIdList.update { emptyList() }
        }
    }

    private fun changeSearchText(newSearchText: String) {
        viewModelScope.launch {
            changeSearchTextDebounce { searchText.update { newSearchText } }
        }
    }

    private fun checkItem(audioMediaId: Int) {
        checkedAudioMediaIdList.update { if (audioMediaId in it) it - audioMediaId else it + audioMediaId }
    }

    private fun selectAll() {
        val allIds = audioMediaListFlow.value.map { it.id }
        checkedAudioMediaIdList.update { if (it.size == allIds.size) emptyList() else allIds }
    }
}
