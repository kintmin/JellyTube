package com.kintmin.presentation.ui.step

import java.time.LocalDate

sealed interface StepIntent {
    data object OnInit : StepIntent
    data class OnSelectHour(val hour: Int) : StepIntent
    data class OnSelectDate(val date: LocalDate) : StepIntent
}
