package com.kintmin.presentation.ui.step

import java.time.LocalDate

data class StepUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val halfHourlySteps: List<Int> = List(48) { 0 },
    val dailyStepsByDate: Map<LocalDate, Int> = emptyMap(),
    val selectedSlot: Int? = null,
    val chartAnimationKey: Long = 0L,
    val errorMessage: String? = null,
)
