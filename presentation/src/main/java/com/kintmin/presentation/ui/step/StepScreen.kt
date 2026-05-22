package com.kintmin.presentation.ui.step

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StepScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = hiltViewModel<StepViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(StepIntent.OnInit)
    }

    StepScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepScreen(
    uiState: StepUiState,
    navigateToBack: () -> Unit,
    sendIntent: (StepIntent) -> Unit,
) {
    val totalSteps = uiState.dailyStepsByDate[uiState.selectedDate] ?: uiState.halfHourlySteps.sum()
    val peakSlot = uiState.halfHourlySteps
        .withIndex()
        .maxByOrNull { it.value }
        ?.index
        ?: 0
    val peakStartHour = peakSlot / 2
    val peakStartMin = if (peakSlot % 2 == 0) 0 else 30
    val peakEndMin = peakStartMin + 30
    val peakEndHour = if (peakEndMin >= 60) (peakStartHour + 1) % 24 else peakStartHour
    val peakHourRangeText = "%02d:%02d~%02d:%02d".format(
        peakStartHour, peakStartMin,
        peakEndHour, peakEndMin % 60,
    )
    val dateText = uiState.selectedDate.format(
        DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090C12),
        topBar = {
            TopAppBar(
                title = { Text("걸음수 현황") },
                navigationIcon = {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            StepDailySummaryView(
                dateText = dateText,
                totalSteps = totalSteps,
                peakHourRangeText = peakHourRangeText,
            )

            Spacer(modifier = Modifier.height(16.dp))

            StepHourlyChartView(
                hourlySteps = uiState.halfHourlySteps,
                selectedHour = uiState.selectedSlot,
                animationKey = uiState.chartAnimationKey,
                onSelectHour = { slot ->
                    sendIntent(StepIntent.OnSelectHour(slot))
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            StepCalendarView(
                selectedDate = uiState.selectedDate,
                dailyStepsByDate = uiState.dailyStepsByDate,
                onSelectDate = { date ->
                    sendIntent(StepIntent.OnSelectDate(date))
                },
                onChangeMonth = { yearMonth ->
                    val currentDay = uiState.selectedDate.dayOfMonth
                    val safeDay = currentDay.coerceAtMost(yearMonth.lengthOfMonth())
                    sendIntent(StepIntent.OnSelectDate(yearMonth.atDay(safeDay)))
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepScreenPreview() {
    JellyTubeTheme {
        StepScreen(
            uiState = StepUiState(
                isLoading = false,
                halfHourlySteps = listOf(
                    50, 80, 60, 100, 200, 300, 250, 400, 600, 700, 500, 900,
                    900, 1200, 1800, 2000, 2600, 3000, 4100, 3500, 3800, 3200,
                    2400, 2000, 1500, 1200, 900, 700, 800, 600, 1200, 1000,
                    1700, 1400, 2200, 2600, 3100, 3500, 3900, 3400, 2700, 2200,
                    1600, 1300, 700, 400, 200, 100,
                ),
                dailyStepsByDate = mapOf(
                    LocalDate.of(2026, 5, 1) to 3120,
                    LocalDate.of(2026, 5, 2) to 8123,
                    LocalDate.of(2026, 5, 3) to 5240,
                    LocalDate.of(2026, 5, 4) to 960,
                    LocalDate.of(2026, 5, 5) to 11021,
                ),
                selectedSlot = 22,
                selectedDate = YearMonth.of(2026, 5).atDay(3),
            ),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
