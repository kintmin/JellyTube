package com.kintmin.presentation.ui.step

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class StepUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val halfHourlySteps: List<Int> = List(48) { 0 },
    val dailyStepsByDate: Map<LocalDate, Int> = emptyMap(),
    val selectedSlot: Int? = null,
    val chartAnimationKey: Long = 0L,
    val errorMessage: String? = null,
)
