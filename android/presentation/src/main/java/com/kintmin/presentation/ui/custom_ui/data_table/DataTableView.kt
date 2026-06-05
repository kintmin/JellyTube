package com.kintmin.presentation.ui.custom_ui.data_table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlin.math.roundToInt

interface ColumnType<T> {

    val headerWidth: Dp

    @Composable
    fun makeHeader(headerHeight: Dp)

    @Composable
    fun makeCell(cellHeight: Dp, data: T)
}

/**
 * 가로, 세로를 전부 LazyColumn, LazyRow로 만들면 동기화가 매우 힘든 이슈 존재.
 * 따라서 아래 구조 이용.
 * 세로: LazyColumn
 * 가로: 그냥 Row지만, 현재 보이는 Column의 개수를 제한하여 최적화.
 */
@Composable
fun <T> DataTableView(
    modifier: Modifier,
    dataList: List<T>,
    keySelector: (T) -> Any,
    fixedHeaderList: List<ColumnType<T>>,
    flexibleHeaderList: List<ColumnType<T>>,
    headerHeight: Dp = 56.dp,
    cellHeight: Dp = 48.dp,
) {
    val horizontalScrollState = rememberScrollState()
    val density = LocalDensity.current

    val fixedWidth = remember(fixedHeaderList) {
        fixedHeaderList.sumOf { it.headerWidth.value.toDouble() }.dp
    }
    val flexibleTotalWidth = remember(flexibleHeaderList) {
        flexibleHeaderList.sumOf { it.headerWidth.value.toDouble() }.dp
    }

    // 각 Column의 가로 길이를 메모이제이션
    val flexiblePrefixWidthsPx = remember(flexibleHeaderList, density) {
        IntArray(flexibleHeaderList.size + 1).apply {
            for (index in flexibleHeaderList.indices) {
                this[index + 1] = this[index] + with(density) { flexibleHeaderList[index].headerWidth.roundToPx() }
            }
        }
    }

    BoxWithConstraints(modifier = modifier) boxScope@{
        val maxWidthPx = with(density) { this@boxScope.maxWidth.roundToPx() }
        val fixedWidthPx = with(density) { fixedWidth.roundToPx() }
        val flexibleViewportWidthPx = (maxWidthPx - fixedWidthPx).coerceAtLeast(0)

        // 가로 스크롤에 따라 현재 보이는 Column을 계산하기 위한 상태
        val visibleRange by remember(flexibleViewportWidthPx, flexiblePrefixWidthsPx, horizontalScrollState) {
            derivedStateOf {
                findVisibleRange(
                    prefixWidthsPx = flexiblePrefixWidthsPx,
                    scrollPx = horizontalScrollState.value,
                    viewportWidthPx = flexibleViewportWidthPx,
                )
            }
        }
        val startOffsetDp = with(density) { flexiblePrefixWidthsPx.getOrElse(visibleRange.start) { 0 }.toDp() }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
            ) {
                Row(
                    modifier = Modifier
                        .width(fixedWidth)
                        .fillMaxHeight()
                ) {
                    fixedHeaderList.forEach { fixedHeader ->
                        Box(
                            modifier = Modifier
                                .width(fixedHeader.headerWidth)
                                .fillMaxHeight()
                        ) {
                            fixedHeader.makeHeader(headerHeight)
                        }
                    }
                }

                FlexibleHeaderViewport(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState),
                    headerHeight = headerHeight,
                    totalWidth = flexibleTotalWidth,
                    headers = flexibleHeaderList,
                    visibleRange = visibleRange,
                    startOffsetDp = startOffsetDp,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = dataList,
                    key = keySelector,
                ) { rowData ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .width(fixedWidth)
                                .fillMaxHeight()
                        ) {
                            fixedHeaderList.forEach { fixedHeader ->
                                Box(
                                    modifier = Modifier
                                        .width(fixedHeader.headerWidth)
                                        .fillMaxHeight()
                                ) {
                                    fixedHeader.makeCell(cellHeight, rowData)
                                }
                            }
                        }

                        FlexibleBodyViewport(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .horizontalScroll(horizontalScrollState),
                            cellHeight = cellHeight,
                            rowData = rowData,
                            totalWidth = flexibleTotalWidth,
                            headers = flexibleHeaderList,
                            visibleRange = visibleRange,
                            startOffsetDp = startOffsetDp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> FlexibleHeaderViewport(
    modifier: Modifier,
    headerHeight: Dp,
    totalWidth: Dp,
    headers: List<ColumnType<T>>,
    visibleRange: VisibleRange,
    startOffsetDp: Dp,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(totalWidth)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .fillMaxHeight()
            ) {
                for (index in visibleRange.start until visibleRange.endExclusive) {
                    val header = headers[index]
                    Box(
                        modifier = Modifier
                            .width(header.headerWidth)
                            .fillMaxHeight()
                    ) {
                        header.makeHeader(headerHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> FlexibleBodyViewport(
    modifier: Modifier,
    cellHeight: Dp,
    rowData: T,
    totalWidth: Dp,
    headers: List<ColumnType<T>>,
    visibleRange: VisibleRange,
    startOffsetDp: Dp,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(totalWidth)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .offset(x = startOffsetDp)
                    .fillMaxHeight()
            ) {
                for (index in visibleRange.start until visibleRange.endExclusive) {
                    val header = headers[index]
                    Box(
                        modifier = Modifier
                            .width(header.headerWidth)
                            .fillMaxHeight()
                    ) {
                        header.makeCell(cellHeight, rowData)
                    }
                }
            }
        }
    }
}

private data class VisibleRange(
    val start: Int,
    val endExclusive: Int,
)

private fun findVisibleRange(
    prefixWidthsPx: IntArray,
    scrollPx: Int,
    viewportWidthPx: Int,
): VisibleRange {
    val count = prefixWidthsPx.size - 1
    if (count <= 0) return VisibleRange(0, 0)

    val startPx = scrollPx.coerceAtLeast(0)
    val endPx = (startPx + viewportWidthPx).coerceAtLeast(startPx)

    var start = 0
    while (start < count && prefixWidthsPx[start + 1] <= startPx) {
        start++
    }

    var end = start
    while (end < count && prefixWidthsPx[end] < endPx) {
        end++
    }

    // 경계 깜빡임 완화용 버퍼 1컬럼
    end = (end + 1).coerceAtMost(count)

    return VisibleRange(start = start, endExclusive = end)
}

@Preview(showBackground = true)
@Composable
fun MainScreenSearchTabPreview() {
    JellyTubeTheme {
        DataTableView(
            modifier = Modifier.fillMaxSize(),
            dataList = TempData.getMockList(20),
            keySelector = { data -> data.id },
            fixedHeaderList = listOf(NameColumn(), DepartmentColumn()),
            flexibleHeaderList = listOf(AgeColumn(), PaymentColumn(), LineColumn())
        )
    }
}

@Composable
fun DefaultHeader(width: Dp, height: Dp, headerName: String) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(BorderStroke(0.5.dp, Color.Gray))
            .background(Color.Red),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = headerName,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DefaultCell(width: Dp, height: Dp, text: String) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(BorderStroke(0.5.dp, Color.Gray))
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Immutable
data class TempData(
    val id: Int,
    val name: String,
    val age: Int,
    val department: String,
    val payment: Float,
    val line: String,
) {

    companion object {

        fun getMockList(count: Int) = List(count) { index ->
            TempData(
                id = index,
                name = (('A' + (index % 52))).toString(),
                age = 10 + index,
                department = "index $index",
                payment = 995.5f + index * 0.5f,
                line = index.toString().repeat(100)
            )
        }
    }
}

class NameColumn : ColumnType<TempData> {

    override val headerWidth: Dp = 80.dp

    @Composable
    override fun makeHeader(headerHeight: Dp) {
        DefaultHeader(headerWidth, headerHeight, "이름")
    }

    @Composable
    override fun makeCell(cellHeight: Dp, data: TempData) {
        DefaultCell(
            width = headerWidth,
            height = cellHeight,
            text = data.name,
        )
    }
}

class AgeColumn : ColumnType<TempData> {

    override val headerWidth: Dp = 90.dp

    @Composable
    override fun makeHeader(headerHeight: Dp) {
        DefaultHeader(headerWidth, headerHeight, "나이")
    }

    @Composable
    override fun makeCell(cellHeight: Dp, data: TempData) {
        DefaultCell(
            width = headerWidth,
            height = cellHeight,
            text = "${data.age}세",
        )
    }
}

class DepartmentColumn : ColumnType<TempData> {

    override val headerWidth: Dp = 140.dp

    @Composable
    override fun makeHeader(headerHeight: Dp) {
        DefaultHeader(headerWidth, headerHeight, "부서")
    }

    @Composable
    override fun makeCell(cellHeight: Dp, data: TempData) {
        DefaultCell(
            width = headerWidth,
            height = cellHeight,
            text = data.department,
        )
    }
}

class PaymentColumn : ColumnType<TempData> {

    override val headerWidth: Dp = 140.dp

    @Composable
    override fun makeHeader(headerHeight: Dp) {
        DefaultHeader(headerWidth, headerHeight, "금액")
    }

    @Composable
    override fun makeCell(cellHeight: Dp, data: TempData) {
        DefaultCell(
            width = headerWidth,
            height = cellHeight,
            text = "${"%,d".format(data.payment.roundToInt())}원",
        )
    }
}

class LineColumn : ColumnType<TempData> {

    override val headerWidth: Dp = 600.dp

    @Composable
    override fun makeHeader(headerHeight: Dp) {
        DefaultHeader(headerWidth, headerHeight, "추가설명")
    }

    @Composable
    override fun makeCell(cellHeight: Dp, data: TempData) {
        DefaultCell(
            width = headerWidth,
            height = cellHeight,
            text = data.line,
        )
    }
}
