package com.kintmin.presentation.ui.step

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.step.usecase.GetHourlyStepsUseCase
import com.kintmin.domain.step.usecase.GetMonthlyDailyStepsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
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
    private val getMonthlyDailyStepsUseCase: GetMonthlyDailyStepsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepUiState())
    val uiState: StateFlow<StepUiState> = _uiState.asStateFlow()

    fun sendIntent(intent: StepIntent) {
        when (intent) {
            StepIntent.OnInit -> loadStepData(_uiState.value.selectedDate)
            is StepIntent.OnSelectHour -> {
                _uiState.update { current ->
                    current.copy(selectedHour = intent.hour)
                }
            }
            is StepIntent.OnSelectDate -> {
                loadStepData(intent.date)
            }
        }
    }

    private fun loadStepData(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedDate = date,
                    errorMessage = null,
                )
            }

            val targetDate = date.format(DateTimeFormatter.BASIC_ISO_DATE)
            val targetMonth = YearMonth.from(date)
            val hourlyResult = getHourlyStepsUseCase(targetDate)
            val dailyResult = getMonthlyDailyStepsUseCase(targetMonth)

            if (hourlyResult.isFailure) {
                val throwable = hourlyResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedDate = date,
                        errorMessage = throwable?.message ?: "시간별 걸음수를 불러오지 못했습니다.",
                    )
                }
                return@launch
            }

            if (dailyResult.isFailure) {
                val throwable = dailyResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedDate = date,
                        errorMessage = throwable?.message ?: "월별 걸음수를 불러오지 못했습니다.",
                    )
                }
                return@launch
            }

            val hourlySteps = hourlyResult.getOrDefault(List(24) { 0 })
            val dailyStepsByDate = dailyResult.getOrDefault(emptyMap())

            _uiState.update {
                val defaultHour = if (date == LocalDate.now(ZoneId.systemDefault())) {
                    LocalTime.now(ZoneId.systemDefault()).hour
                } else {
                    0
                }

                it.copy(
                    isLoading = false,
                    selectedDate = date,
                    hourlySteps = hourlySteps,
                    dailyStepsByDate = dailyStepsByDate,
                    selectedHour = it.selectedHour ?: defaultHour,
                    chartAnimationKey = it.chartAnimationKey + 1L,
                    errorMessage = null,
                )
            }
        }
    }
}
