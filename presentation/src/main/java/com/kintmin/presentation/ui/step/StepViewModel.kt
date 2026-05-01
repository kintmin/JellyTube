package com.kintmin.presentation.ui.step

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.step.usecase.GetHourlyStepsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StepViewModel @Inject constructor(
    private val getHourlyStepsUseCase: GetHourlyStepsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepUiState())
    val uiState: StateFlow<StepUiState> = _uiState.asStateFlow()

    fun sendIntent(intent: StepIntent) {
        when (intent) {
            StepIntent.OnInit -> loadHourlySteps()
            is StepIntent.OnSelectHour -> {
                _uiState.update { current ->
                    current.copy(selectedHour = intent.hour)
                }
            }
        }
    }

    private fun loadHourlySteps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE)
            val result = getHourlyStepsUseCase(today)

            result
                .onSuccess { hourlySteps ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hourlySteps = hourlySteps,
                            selectedHour = it.selectedHour ?: LocalTime.now(ZoneId.systemDefault()).hour,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "시간별 걸음수를 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }
}
