package com.kintmin.presentation.ui.step

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun StepHourlyChartView(
    hourlySteps: List<Int>,
    selectedHour: Int?,
    onSelectHour: (Int) -> Unit,
    modifier: Modifier = Modifier,
    animationKey: Any = Unit,
) {
    val maxStep = (hourlySteps.maxOrNull() ?: 0).coerceAtLeast(1)
    val barAreaHeight = 180.dp
    val minBarHeight = 6.dp
    val tooltipHeight = 40.dp
    val tooltipTopAreaHeight = 56.dp
    val density = LocalDensity.current
    var chartWidthPx by remember { mutableFloatStateOf(0f) }
    var bubbleWidthPx by remember { mutableFloatStateOf(112f) }
    val animatedProgress = remember(animationKey) { Animatable(0f) }

    LaunchedEffect(animationKey) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E121A))
            .padding(horizontal = 8.dp, vertical = 20.dp)
            .onSizeChanged { size ->
                chartWidthPx = size.width.toFloat()
            },
    ) {
        fun hourFromXPosition(x: Float): Int {
            if (chartWidthPx <= 0f) return 0
            val raw = floor(x / (chartWidthPx / 24f)).toInt()
            return raw.coerceIn(0, 23)
        }

        val chartWidthDp = with(density) { chartWidthPx.toDp() }
        val slotWidth = if (chartWidthPx > 0f) chartWidthDp / 24f else 0.dp

        Column {
            Spacer(modifier = Modifier.height(tooltipTopAreaHeight))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barAreaHeight + 52.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitPointerEvent().changes.firstOrNull() ?: continue
                                if (!down.pressed) continue

                                onSelectHour(hourFromXPosition(down.position.x))

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    onSelectHour(hourFromXPosition(change.position.x))
                                }
                            }
                        }
                    },
            ) {
                hourlySteps.forEachIndexed { hour, steps ->
                    val selected = selectedHour == hour
                    val barHeight = getBarHeightDp(
                        steps = steps,
                        maxStep = maxStep,
                        maxBarHeight = barAreaHeight,
                        minBarHeight = minBarHeight,
                        progress = animatedProgress.value,
                    )
                    val interactionSource = remember(hour) { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                onSelectHour(hour)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(barAreaHeight - barHeight))

                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selected) Color(0xFF43E38E) else Color(0xFF29D174),
                                ),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = hourLabel(hour),
                            color = if (selected && hour != 23) Color(0xFFD7FFE9) else Color(0xFF8E96A4),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        val selectedSteps = selectedHour?.let { hour -> hourlySteps.getOrNull(hour) ?: 0 }
        if (selectedHour != null && selectedSteps != null && chartWidthPx > 0f) {
            val selectedBarHeight = getBarHeightDp(
                steps = selectedSteps,
                maxStep = maxStep,
                maxBarHeight = barAreaHeight,
                minBarHeight = minBarHeight,
                progress = animatedProgress.value,
            )

            val safePadding = 4.dp
            val bubbleWidthDp = with(density) { bubbleWidthPx.toDp() }
            val maxBubbleStart = (chartWidthDp - bubbleWidthDp - safePadding).coerceAtLeast(safePadding)
            val barCenterX = (slotWidth * selectedHour) + (slotWidth / 2f)
            val bubbleStartX = (barCenterX - bubbleWidthDp / 2f).coerceIn(safePadding, maxBubbleStart)
            val connectorHeight = (
                tooltipTopAreaHeight + (barAreaHeight - selectedBarHeight) - tooltipHeight
                ).coerceAtLeast(8.dp)

            SelectedHourTooltip(
                modifier = Modifier
                    .offset(x = bubbleStartX)
                    .onSizeChanged { size ->
                        bubbleWidthPx = size.width.toFloat()
                    },
                hour = selectedHour,
                steps = selectedSteps,
                connectorX = barCenterX - bubbleStartX,
                connectorHeight = connectorHeight,
                maxBubbleWidth = (chartWidthDp - (safePadding * 2f)).coerceAtLeast(120.dp),
            )
        }
    }
}

@Composable
private fun SelectedHourTooltip(
    modifier: Modifier,
    hour: Int,
    steps: Int,
    connectorX: Dp,
    connectorHeight: Dp,
    maxBubbleWidth: Dp,
) {
    val bubbleColor = Color(0xFF2C6C52)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .widthIn(min = 120.dp, max = maxBubbleWidth)
                .height(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "%02d:00  ${steps}걸음".format(hour),
                color = Color(0xFFE9FFF4),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .offset(x = connectorX, y = 40.dp)
                .height(connectorHeight)
                .width(1.dp)
                .background(bubbleColor),
        )
    }
}

private fun getBarHeightDp(
    steps: Int,
    maxStep: Int,
    maxBarHeight: Dp,
    minBarHeight: Dp,
    progress: Float,
): Dp {
    val ratio = (steps.toFloat() / maxStep.toFloat()).coerceIn(0f, 1f)
    val animatedRatio = (ratio * progress).coerceIn(0f, 1f)
    val barHeight = (animatedRatio * maxBarHeight.value).roundToInt().dp
    return barHeight.coerceAtLeast(minBarHeight)
}

private fun hourLabel(hour: Int): String = when (hour) {
    0 -> "0시"
    6 -> "6시"
    12 -> "12시"
    18 -> "18시"
    23 -> "시"
    else -> ""
}

@Preview(showBackground = true)
@Composable
private fun StepHourlyChartViewPreview() {
    JellyTubeTheme {
        StepHourlyChartView(
            hourlySteps = listOf(
                100, 200, 130, 400, 600, 1200,
                500, 900, 1800, 2600, 4100, 3800,
                2400, 1500, 900, 800, 1200, 1700,
                2200, 3100, 3900, 2700, 1600, 700,
            ),
            selectedHour = 11,
            onSelectHour = {},
        )
    }
}
