package com.kintmin.presentation.ui.step

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun StepCalendarView(
    selectedDate: LocalDate,
    dailyStepsByDate: Map<LocalDate, Int>,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentMonth = YearMonth.from(selectedDate)
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayCount = currentMonth.lengthOfMonth()
    val leadingBlank = (firstDayOfMonth.dayOfWeek.value % 7)

    val cells = remember(currentMonth) {
        buildList {
            repeat(leadingBlank) { add(null) }
            repeat(dayCount) { day -> add(currentMonth.atDay(day + 1)) }
            while (size % 7 != 0) {
                add(null)
            }
        }
    }

    var dragAccumulated = 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0E121A))
            .pointerInput(currentMonth) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulated += dragAmount
                    },
                    onDragEnd = {
                        when {
                            dragAccumulated > 80f -> onChangeMonth(currentMonth.minusMonths(1))
                            dragAccumulated < -80f -> onChangeMonth(currentMonth.plusMonths(1))
                        }
                        dragAccumulated = 0f
                    },
                    onDragCancel = {
                        dragAccumulated = 0f
                    },
                )
            }
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = { onChangeMonth(currentMonth.minusMonths(1)) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                    contentDescription = "이전 달",
                    tint = Color(0xFFE8ECF2),
                )
            }

            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월")),
                color = Color(0xFFE8ECF2),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { onChangeMonth(currentMonth.plusMonths(1)) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = "다음 달",
                    tint = Color(0xFFE8ECF2),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                Text(
                    modifier = Modifier.weight(1f),
                    text = day,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF7F8794),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEach { date ->
                    val isSelected = date == selectedDate
                    val dayColor = when (date?.dayOfWeek) {
                        DayOfWeek.SUNDAY -> Color(0xFFE06E7A)
                        DayOfWeek.SATURDAY -> Color(0xFF7A8BEA)
                        else -> Color(0xFFD0D6E0)
                    }
                    val daySteps = date?.let { dailyStepsByDate[it] ?: 0 }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF2D6F57) else Color.Transparent)
                            .let {
                                if (date != null) {
                                    it.clickable { onSelectDate(date) }
                                } else {
                                    it
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (date != null) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (isSelected) Color(0xFFD7FFE9) else dayColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )

                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = "${daySteps ?: 0}",
                                color = if (isSelected) Color(0xFFC7FFD8) else Color(0xFF7F8794),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepCalendarViewPreview() {
    JellyTubeTheme {
        StepCalendarView(
            selectedDate = LocalDate.of(2026, 5, 3),
            dailyStepsByDate = mapOf(
                LocalDate.of(2026, 5, 1) to 3120,
                LocalDate.of(2026, 5, 2) to 8123,
                LocalDate.of(2026, 5, 3) to 5240,
                LocalDate.of(2026, 5, 4) to 960,
                LocalDate.of(2026, 5, 5) to 11021,
                LocalDate.of(2026, 5, 6) to 7380,
                LocalDate.of(2026, 5, 7) to 4290,
            ),
            onSelectDate = {},
            onChangeMonth = {},
        )
    }
}
