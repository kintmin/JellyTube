package com.kintmin.presentation.remember_state

import org.junit.Assert.assertEquals
import org.junit.Test

private data class Row(val id: Int, val label: String = "")

class ReorderStateTest {

    @Test
    fun `맨 위 항목은 위로 끌어도 제 슬롯 위로 올라가지 않는다`() {
        assertEquals(0f, clampDragOffsetY(offset = -120f, index = 0, lastIndex = 4), 0f)
        assertEquals(80f, clampDragOffsetY(offset = 80f, index = 0, lastIndex = 4), 0f)
    }

    @Test
    fun `맨 아래 항목은 아래로 끌어도 제 슬롯 아래로 내려가지 않는다`() {
        assertEquals(0f, clampDragOffsetY(offset = 120f, index = 4, lastIndex = 4), 0f)
        assertEquals(-80f, clampDragOffsetY(offset = -80f, index = 4, lastIndex = 4), 0f)
    }

    @Test
    fun `항목이 하나면 위아래 양쪽이 모두 잠긴다`() {
        assertEquals(0f, clampDragOffsetY(offset = -120f, index = 0, lastIndex = 0), 0f)
        assertEquals(0f, clampDragOffsetY(offset = 120f, index = 0, lastIndex = 0), 0f)
    }

    @Test
    fun `가운데 항목은 양쪽으로 자유롭게 움직인다`() {
        assertEquals(-120f, clampDragOffsetY(offset = -120f, index = 2, lastIndex = 4), 0f)
        assertEquals(120f, clampDragOffsetY(offset = 120f, index = 2, lastIndex = 4), 0f)
    }

    @Test
    fun `끄는 중에 도착한 목록은 손에 쥔 배치를 덮지 않는다`() {
        val inHand = listOf(Row(3), Row(1), Row(2))
        val incoming = listOf(Row(1), Row(2), Row(3))

        val merged = mergeKeepingOrder(inHand, incoming, Row::id)

        assertEquals(listOf(3, 1, 2), merged.map { it.id })
    }

    @Test
    fun `끄는 중에 바뀐 내용은 배치를 유지한 채 반영된다`() {
        val inHand = listOf(Row(3, "옛것"), Row(1, "옛것"))
        val incoming = listOf(Row(1, "새것"), Row(3, "새것"))

        val merged = mergeKeepingOrder(inHand, incoming, Row::id)

        assertEquals(listOf(3, 1), merged.map { it.id })
        assertEquals(listOf("새것", "새것"), merged.map { it.label })
    }

    @Test
    fun `끄는 중에 늘어난 항목은 뒤에 붙는다`() {
        val inHand = listOf(Row(3), Row(1))
        val incoming = listOf(Row(1), Row(3), Row(9))

        val merged = mergeKeepingOrder(inHand, incoming, Row::id)

        assertEquals(listOf(3, 1, 9), merged.map { it.id })
    }

    @Test
    fun `끄는 중에 사라진 항목은 빠진다`() {
        val inHand = listOf(Row(3), Row(1), Row(2))
        val incoming = listOf(Row(1), Row(2))

        val merged = mergeKeepingOrder(inHand, incoming, Row::id)

        assertEquals(listOf(1, 2), merged.map { it.id })
    }

    private fun direction(
        itemTop: Float,
        dragDirection: Int,
        viewStart: Float = 0f,
        canMoveUp: Boolean = true,
        canMoveDown: Boolean = true,
    ) = autoScrollVelocity(
        itemTop = itemTop,
        itemBottom = itemTop + 80f,
        viewStart = viewStart,
        viewEnd = 1000f,
        scrollZone = 100f,
        dragDirection = dragDirection,
        canMoveUp = canMoveUp,
        canMoveDown = canMoveDown,
        canScrollBackward = true,
        canScrollForward = true,
    )

    @Test
    fun `위쪽 존에서 위로 끌면 손가락이 멈춰도 위로 스크롤한다`() {
        assertEquals(-0.5f, direction(itemTop = 50f, dragDirection = -1), 0f)
    }

    @Test
    fun `위쪽 존에서 손가락을 아래로 돌리면 스크롤을 멈춘다`() {
        assertEquals(0f, direction(itemTop = 50f, dragDirection = 1), 0f)
    }

    @Test
    fun `상단 바에 덮인 만큼 위쪽 존이 내려온다`() {
        assertEquals(0f, direction(itemTop = 150f, dragDirection = -1), 0f)
        assertEquals(-0.5f, direction(itemTop = 150f, dragDirection = -1, viewStart = 100f), 0f)
    }

    @Test
    fun `넘어갈 이웃이 없는 방향으로는 스크롤하지 않는다`() {
        assertEquals(0f, direction(itemTop = 50f, dragDirection = -1, canMoveUp = false), 0f)
        assertEquals(0f, direction(itemTop = 900f, dragDirection = 1, canMoveDown = false), 0f)
    }

    @Test
    fun `아래쪽 존에서 아래로 끌면 아래로 스크롤한다`() {
        assertEquals(0.8f, direction(itemTop = 900f, dragDirection = 1), 0.0001f)
    }

    @Test
    fun `존에 막 들어서면 최저 속도부터 시작하고 깊을수록 빨라진다`() {
        assertEquals(-0.25f, direction(itemTop = 95f, dragDirection = -1), 0f)
        assertEquals(-1f, direction(itemTop = -20f, dragDirection = -1), 0f)
    }
}
