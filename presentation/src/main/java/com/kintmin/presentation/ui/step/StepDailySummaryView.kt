package com.kintmin.presentation.ui.step

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun StepDailySummaryView(
    dateText: String,
    totalSteps: Int,
    peakHourRangeText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0E121A))
            .heightIn(min = 116.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE8ECF2),
            fontWeight = FontWeight.Bold,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "최종 걸음수",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E96A4),
                )
                Text(
                    text = "${totalSteps}걸음",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFD7FFE9),
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "가장 많이 걸은 시간대",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E96A4),
                )
                Text(
                    text = peakHourRangeText,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE8ECF2),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepDailySummaryViewPreview() {
    JellyTubeTheme {
        StepDailySummaryView(
            dateText = "2026년 5월 3일",
            totalSteps = 5240,
            peakHourRangeText = "18:00~19:00",
        )
    }
}
