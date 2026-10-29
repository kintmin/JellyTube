package com.kintmin.presentation.ui.setting.device_save_select

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.model.AudioExportTarget
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.AudioMediaExportSummary
import com.kintmin.domain.audio_media.usecase.ExportAudioMediaListToDeviceUseCase
import com.kintmin.domain.audio_media.usecase.FetchAllAudioMediaListFlowUseCase
import com.kintmin.presentation.extension.matchKorean
import com.kintmin.presentation.ui.setting.device_save_select.list.toSettingDeviceSaveSelectListItemUiState
import com.kintmin.presentation.util.Debounce
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingDeviceSaveSelectViewModel constructor(
    fetchAllAudioMediaListFlowUseCase: FetchAllAudioMediaListFlowUseCase,
    private val exportAudioMediaListToDeviceUseCase: ExportAudioMediaListToDeviceUseCase,
) : ViewModel() {

    private val eventChannel = Channel<SettingDeviceSaveSelectEvent>(capacity = 8)
    val eventFlow = eventChannel.receiveAsFlow()

    private val checkedItemIdList = MutableStateFlow(listOf<Int>())
    private val selectedTargetList = MutableStateFlow(SettingDeviceSaveSelectUiState.DEFAULT_TARGET_LIST)
    private val isSaving = MutableStateFlow(false)
    private val searchText = MutableStateFlow("")

    private val changeSearchTextDebounce = Debounce(200L)

    /** 저장 대상으로 넘길 도메인 모델. UiState에는 표시용 필드만 있어서 원본을 따로 들고 있는다. */
    private val audioMediaListFlow: StateFlow<List<AudioMedia>> = fetchAllAudioMediaListFlowUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SettingDeviceSaveSelectUiState> = combine(
        audioMediaListFlow,
        checkedItemIdList,
        searchText,
        selectedTargetList,
        isSaving,
    ) { audioMediaList, checkedIds, keyword, targetList, isSavingNow ->
        SettingDeviceSaveSelectUiState(
            audioMediaList = audioMediaList
                .filter { it.name.matchKorean(keyword) }
                .map { it.toSettingDeviceSaveSelectListItemUiState(isChecked = it.id in checkedIds) },
            checkedItemCount = checkedIds.size,
            selectedTargetList = targetList,
            isSaving = isSavingNow,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingDeviceSaveSelectUiState())

    fun sendIntent(intent: SettingDeviceSaveSelectIntent) {
        when (intent) {
            is SettingDeviceSaveSelectIntent.OnChangeSearchText -> changeSearchText(intent.searchText)
            is SettingDeviceSaveSelectIntent.OnClickAudioItem -> checkItem(intent.data.id)
            is SettingDeviceSaveSelectIntent.OnToggleExportTarget -> toggleExportTarget(intent.target)
            SettingDeviceSaveSelectIntent.OnClickClearCheckedItemList -> checkedItemIdList.update { emptyList() }
            SettingDeviceSaveSelectIntent.OnClickSaveToDevice -> saveCheckedItemListToDevice()
        }
    }

    private fun changeSearchText(newSearchText: String) {
        viewModelScope.launch {
            changeSearchTextDebounce {
                searchText.update { newSearchText }
            }
        }
    }

    private fun checkItem(audioMediaId: Int) {
        checkedItemIdList.update {
            if (it.contains(audioMediaId)) it - audioMediaId else it + audioMediaId
        }
    }

    private fun toggleExportTarget(target: AudioExportTarget) {
        selectedTargetList.update {
            if (it.contains(target)) it - target else it + target
        }
    }

    private fun saveCheckedItemListToDevice() {
        if (isSaving.value) return

        val targetIdList = checkedItemIdList.value
        val targetList = selectedTargetList.value
        if (targetIdList.isEmpty() || targetList.isEmpty()) return

        val exportTargetMediaList = audioMediaListFlow.value.filter { it.id in targetIdList }
        if (exportTargetMediaList.isEmpty()) return

        viewModelScope.launch {
            isSaving.value = true
            exportAudioMediaListToDeviceUseCase(exportTargetMediaList, targetList)
                .onSuccess { summary ->
                    checkedItemIdList.update { emptyList() }
                    eventChannel.send(SettingDeviceSaveSelectEvent.ShowToast(summary.toMessage()))
                }
                .onFailure { throwable ->
                    eventChannel.send(
                        SettingDeviceSaveSelectEvent.ShowToast(
                            throwable.message ?: "기기에 저장하지 못했습니다.",
                        )
                    )
                }
            isSaving.value = false
        }
    }
}

private fun AudioMediaExportSummary.toMessage(): String = buildList {
    if (exportedCount > 0) add("${exportedCount}개를 기기에 저장했습니다.")
    if (alreadyExportedCount > 0) add("${alreadyExportedCount}개는 이미 저장되어 있습니다.")
    if (failedCount > 0) add("${failedCount}개는 저장하지 못했습니다.")
}.joinToString("\n").ifBlank { "저장할 음원이 없습니다." }
