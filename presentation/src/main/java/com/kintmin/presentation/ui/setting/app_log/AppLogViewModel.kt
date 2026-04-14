package com.kintmin.presentation.ui.setting.app_log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_log.usecase.FetchAppLogDateListUseCase
import com.kintmin.domain.app_log.usecase.FetchAppLogLineListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppLogViewModel @Inject constructor(
    private val fetchAppLogDateListUseCase: FetchAppLogDateListUseCase,
    private val fetchAppLogLineListUseCase: FetchAppLogLineListUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLogUiState())
    val uiState = _uiState.asStateFlow()

    private var fullLogLineList: List<String> = emptyList()
    private val pageSize = 200

    fun sendIntent(intent: AppLogIntent) {
        when (intent) {
            AppLogIntent.OnInit -> initialize()
            is AppLogIntent.OnClickLogDate -> selectLogDate(intent.date)
            AppLogIntent.OnRequestNextPage -> loadNextPage()
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val logDateList = fetchAppLogDateListUseCase().getOrElse { emptyList() }
            val firstDate = logDateList.firstOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    logDateList = logDateList,
                    selectedLogDate = firstDate,
                )
            }
            firstDate?.let { date -> loadLineList(date) }
        }
    }

    private fun selectLogDate(date: String) {
        if (_uiState.value.selectedLogDate == date) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedLogDate = date,
                    logLineList = emptyList(),
                    hasNextPage = false,
                    isLoading = true,
                )
            }
            loadLineList(date)
        }
    }

    private suspend fun loadLineList(date: String) {
        fullLogLineList = fetchAppLogLineListUseCase(date).getOrElse { emptyList() }
        val firstPageLineList = fullLogLineList.take(pageSize)
        _uiState.update {
            it.copy(
                isLoading = false,
                logLineList = firstPageLineList,
                hasNextPage = firstPageLineList.size < fullLogLineList.size,
            )
        }
    }

    private fun loadNextPage() {
        val currentUiState = _uiState.value
        if (currentUiState.isLoading || !currentUiState.hasNextPage) return

        val nextPageEndIndex = (currentUiState.logLineList.size + pageSize).coerceAtMost(fullLogLineList.size)
        val nextPageLineList = fullLogLineList.take(nextPageEndIndex)
        _uiState.update {
            it.copy(
                logLineList = nextPageLineList,
                hasNextPage = nextPageEndIndex < fullLogLineList.size,
            )
        }
    }
}
