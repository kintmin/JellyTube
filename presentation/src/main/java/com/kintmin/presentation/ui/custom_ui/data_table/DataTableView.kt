package com.kintmin.presentation.ui.custom_ui.data_table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val fixedWidth = fixedHeaderList.sumOf { it.headerWidth.value.toDouble() }.dp
    val flexibleContentWidth = flexibleHeaderList.sumOf { it.headerWidth.value.toDouble() }.dp

    Column(modifier = modifier) {
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

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Row(modifier = Modifier.width(flexibleContentWidth)) {
                    flexibleHeaderList.forEach { flexibleHeader ->
                        Box(
                            modifier = Modifier
                                .width(flexibleHeader.headerWidth)
                                .fillMaxHeight()
                        ) {
                            flexibleHeader.makeHeader(headerHeight)
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(
                items = dataList,
                key = { _, data -> keySelector(data) }
            ) { _, rowData ->

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

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Row(modifier = Modifier.width(flexibleContentWidth)) {
                            flexibleHeaderList.forEach { flexibleHeader ->
                                Box(
                                    modifier = Modifier
                                        .width(flexibleHeader.headerWidth)
                                        .fillMaxHeight()
                                ) {
                                    flexibleHeader.makeCell(cellHeight, rowData)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
        Text(headerName, color = Color.White)
    }
}

@Composable
fun DefaultCell(width: Dp, height: Dp, data: TempData, selector: (TempData) -> String) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(BorderStroke(0.5.dp, Color.Gray))
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center,
    ) {
        Text(selector(data))
    }
}


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
        DefaultCell(headerWidth, cellHeight, data) { value -> value.name }
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
        DefaultCell(headerWidth, cellHeight, data) { value -> "${value.age}세" }
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
        DefaultCell(headerWidth, cellHeight, data) { value -> value.department }
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
            headerWidth,
            cellHeight,
            data
        ) { value -> "${"%,d".format(value.payment.roundToInt())}원" }
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
        DefaultCell(headerWidth, cellHeight, data) { value -> value.line }
    }
}
