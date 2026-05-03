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
    val totalSteps = uiState.dailyStepsByDate[uiState.selectedDate] ?: uiState.hourlySteps.sum()
    val peakHour = uiState.hourlySteps
        .withIndex()
        .maxByOrNull { it.value }
        ?.index
        ?: 0
    val peakHourRangeText = "%02d:00~%02d:00".format(peakHour, (peakHour + 1) % 24)
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
                hourlySteps = uiState.hourlySteps,
                selectedHour = uiState.selectedHour,
                animationKey = uiState.chartAnimationKey,
                onSelectHour = { hour ->
                    sendIntent(StepIntent.OnSelectHour(hour))
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
                hourlySteps = listOf(
                    100, 200, 130, 400, 600, 1200,
                    500, 900, 1800, 2600, 4100, 3800,
                    2400, 1500, 900, 800, 1200, 1700,
                    2200, 3100, 3900, 2700, 1600, 700,
                ),
                dailyStepsByDate = mapOf(
                    LocalDate.of(2026, 5, 1) to 3120,
                    LocalDate.of(2026, 5, 2) to 8123,
                    LocalDate.of(2026, 5, 3) to 5240,
                    LocalDate.of(2026, 5, 4) to 960,
                    LocalDate.of(2026, 5, 5) to 11021,
                ),
                selectedHour = 11,
                selectedDate = YearMonth.of(2026, 5).atDay(3),
            ),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
