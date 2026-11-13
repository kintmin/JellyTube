package com.kintmin.presentation.ui.step

import com.kintmin.domain.step.StepSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class StepUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val halfHourlySteps: List<Int> = List(StepSlot.PER_DAY) { 0 },
    val dailyStepsByDate: Map<LocalDate, Int> = emptyMap(),
    val selectedSlot: Int? = null,
    val chartAnimationKey: Long = 0L,
    val errorMessage: String? = null,
)
