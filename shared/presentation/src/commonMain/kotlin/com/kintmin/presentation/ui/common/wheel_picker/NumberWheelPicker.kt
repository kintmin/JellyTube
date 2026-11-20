package com.kintmin.presentation.ui.common.wheel_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import kotlin.math.abs

/**
 * 별도 라이브러리 없이 LazyColumn + 스냅으로 구현한 숫자 휠 피커.
 * 뷰포트 중앙에 가장 가까운 값을 선택값으로 보고 onValueChange 로 알린다.
 */
internal val WHEEL_ITEM_HEIGHT = 40.dp
internal const val WHEEL_VISIBLE_COUNT = 5

// 가운데에서 한 칸 벗어난 값의 크기. 크기·투명도를 거리에 비례해 바꿔야 선택이 툭 튀지 않는다.
private const val OFF_CENTER_SCALE = 0.75f
private const val EDGE_ALPHA = 0.3f

@Composable
fun NumberWheelPicker(
    values: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    showHighlight: Boolean = true,
    formatter: (Int) -> String = { it.toString().padStart(2, '0') },
) {
    val halfCount = WHEEL_VISIBLE_COUNT / 2
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf initialIndex
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info ->
                abs((info.offset + info.size / 2) - viewportCenter)
            }?.index ?: initialIndex
        }
    }

    LaunchedEffect(centerIndex) {
        values.getOrNull(centerIndex)?.let(onValueChange)
    }

    // 스크롤 중에는 건드리지 않는다 — 휠이 만든 값이 한 프레임 늦게 돌아와 휠과 싸운다.
    LaunchedEffect(selectedValue) {
        val index = values.indexOf(selectedValue)
        if (index >= 0 && index != centerIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = gray60,
            )
            Spacer(Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_ITEM_HEIGHT * WHEEL_VISIBLE_COUNT),
            contentAlignment = Alignment.Center,
        ) {
            if (showHighlight) {
                WheelPickerHighlight()
            }
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = WHEEL_ITEM_HEIGHT * halfCount),
                modifier = Modifier.fillMaxHeight(),
            ) {
                itemsIndexed(values) { index, value ->
                    Box(
                        modifier = Modifier
                            .height(WHEEL_ITEM_HEIGHT)
                            .fillMaxWidth()
                            // 그리기 단계에서만 읽어 스크롤마다 재구성하지 않는다.
                            .graphicsLayer {
                                val info = listState.layoutInfo
                                val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                                    ?: return@graphicsLayer
                                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                val distance = abs(item.offset + item.size / 2f - viewportCenter) / item.size
                                val scale = lerp(1f, OFF_CENTER_SCALE, distance.coerceAtMost(1f))
                                scaleX = scale
                                scaleY = scale
                                alpha = lerp(1f, EDGE_ALPHA, (distance / halfCount).coerceAtMost(1f))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = formatter(value),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** 선택 줄을 가리키는 띠. 여러 휠을 나란히 둘 때는 휠마다 끊기지 않게 바깥에서 하나만 그린다. */
@Composable
fun WheelPickerHighlight(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WHEEL_ITEM_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            // 시트 배경 위에 다른 색상을 얹지 않고 본문색 state layer(M3 선택 8%)로 올린다.
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    )
}

@Preview
@Composable
private fun WheelPickerHighlightPreview() {
    JellyTubeTheme {
        WheelPickerHighlight()
    }
}

@Preview
@Composable
private fun NumberWheelPickerPreview() {
    JellyTubeTheme {
        NumberWheelPicker(
            values = (0..59).toList(),
            selectedValue = 12,
            onValueChange = {},
            label = "초",
            modifier = Modifier.width(72.dp),
        )
    }
}
