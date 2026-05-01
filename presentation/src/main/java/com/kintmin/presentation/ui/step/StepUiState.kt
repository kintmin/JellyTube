package com.kintmin.presentation.ui.step

data class StepUiState(
    val isLoading: Boolean = true,
    val hourlySteps: List<Int> = List(24) { 0 },
    val selectedHour: Int? = null,
    val errorMessage: String? = null,
) {
    val totalSteps: Int
        get() = hourlySteps.sum()

    val averageSteps: Int
        get() = if (hourlySteps.isEmpty()) 0 else hourlySteps.sum() / hourlySteps.size
}
