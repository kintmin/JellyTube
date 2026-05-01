package com.kintmin.presentation.ui.step

sealed interface StepIntent {
    data object OnInit : StepIntent
    data class OnSelectHour(val hour: Int) : StepIntent
}
