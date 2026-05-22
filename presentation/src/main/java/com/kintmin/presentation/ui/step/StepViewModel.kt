package com.kintmin.presentation.ui.step

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.step.usecase.GetHalfHourlyStepsUseCase
import com.kintmin.domain.step.usecase.GetMonthlyDailyStepsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StepViewModel @Inject constructor(
    private val getHalfHourlyStepsUseCase: GetHalfHourlyStepsUseCase,
    private val getMonthlyDailyStepsUseCase: GetMonthlyDailyStepsUseCase,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedSlot = MutableStateFlow<Int?>(currentHalfHourlySlot())
    private val _chartAnimationKey = MutableStateFlow(0L)

    private val halfHourlyStepsFlow = _selectedDate.flatMapLatest { date ->
        getHalfHourlyStepsUseCase(date.format(DateTimeFormatter.BASIC_ISO_DATE))
    }

    private val monthlyStepsFlow = _selectedDate.map { YearMonth.from(it) }
        .flatMapLatest { yearMonth ->
            getMonthlyDailyStepsUseCase(yearMonth)
        }

    val uiState: StateFlow<StepUiState> = combine(
        _selectedDate,
        halfHourlyStepsFlow,
        monthlyStepsFlow,
        _selectedSlot,
        _chartAnimationKey,
    ) { selectedDate, halfHourlySteps, dailyStepsByDate, selectedSlot, chartAnimationKey ->
        StepUiState(
            isLoading = false,
            selectedDate = selectedDate,
            halfHourlySteps = halfHourlySteps,
            dailyStepsByDate = dailyStepsByDate,
            selectedSlot = selectedSlot ?: currentHalfHourlySlot(),
            chartAnimationKey = chartAnimationKey,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StepUiState(),
    )

    fun sendIntent(intent: StepIntent) {
        when (intent) {
            StepIntent.OnInit -> Unit
            is StepIntent.OnSelectHour -> {
                _selectedSlot.update { intent.hour }
            }
            is StepIntent.OnSelectDate -> {
                _selectedDate.update { intent.date }
                _chartAnimationKey.update { it + 1L }
            }
        }
    }

    private fun currentHalfHourlySlot(): Int {
        val now = LocalTime.now(ZoneId.systemDefault())
        return now.hour * 2 + now.minute / 30
    }
}
