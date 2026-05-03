package com.kintmin.presentation.ui.step

import java.time.LocalDate

data class StepUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val hourlySteps: List<Int> = List(24) { 0 },
    val dailyStepsByDate: Map<LocalDate, Int> = emptyMap(),
    val selectedHour: Int? = null,
    val chartAnimationKey: Long = 0L,
    val errorMessage: String? = null,
)
