package com.kintmin.presentation.ui.step

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlin.math.roundToInt

@Composable
fun StepScreen() {
    val viewModel = hiltViewModel<StepViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(StepIntent.OnInit)
    }

    StepScreen(
        uiState = uiState,
        sendIntent = viewModel::sendIntent,
    )
}

@Composable
fun StepScreen(
    uiState: StepUiState,
    sendIntent: (StepIntent) -> Unit,
) {
    val maxStep = (uiState.hourlySteps.maxOrNull() ?: 0).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
            .padding(16.dp),
    ) {
        Text(
            text = "24시간 걸음",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFB4BAC4),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${uiState.totalSteps} 걸음",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFE8ECF2),
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "시간 평균 ${uiState.averageSteps} 걸음",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFB4BAC4),
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0E121A))
                    .padding(horizontal = 12.dp, vertical = 20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    uiState.hourlySteps.forEachIndexed { hour, steps ->
                        val selected = uiState.selectedHour == hour
                        val ratio = (steps.toFloat() / maxStep.toFloat()).coerceIn(0f, 1f)
                        val barHeight = (ratio * 180f).roundToInt().coerceAtLeast(6)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            if (selected) {
                                SelectedHourTooltip(
                                    hour = hour,
                                    steps = steps,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                Spacer(modifier = Modifier.height(66.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (selected) Color(0xFF43E38E)
                                        else Color(0xFF29D174)
                                    )
                                    .clickable {
                                        sendIntent(StepIntent.OnSelectHour(hour))
                                    }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                modifier = if (selected) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF2D6F57))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                } else {
                                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                },
                                text = "%02d".format(hour),
                                color = if (selected) Color(0xFFD7FFE9) else Color(0xFF8E96A4),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SelectedHourTooltip(
    hour: Int,
    steps: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2C6C52))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = "%02d:00  ${steps}걸음".format(hour),
                color = Color(0xFFE9FFF4),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(Color(0xFF2C6C52)),
        )
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
                selectedHour = 11,
            ),
            sendIntent = {},
        )
    }
}
