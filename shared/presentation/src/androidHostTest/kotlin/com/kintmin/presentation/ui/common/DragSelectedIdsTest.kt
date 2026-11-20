package com.kintmin.presentation.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class DragSelectedIdsTest {

    private val ids = listOf(10, 20, 30, 40, 50)

    @Test
    fun `선택 안 된 곡에서 시작하면 끈 범위를 모두 고른다`() {
        val result = dragSelectedIds(ids, initialCheckedIds = setOf(50), anchorId = 20, currentId = 40)

        assertEquals(setOf(20, 30, 40, 50), result)
    }

    @Test
    fun `선택된 곡에서 시작하면 끈 범위를 모두 해제한다`() {
        val result = dragSelectedIds(ids, initialCheckedIds = setOf(10, 20, 30, 40), anchorId = 30, currentId = 20)

        assertEquals(setOf(10, 40), result)
    }

    @Test
    fun `범위에서 다시 빠진 곡은 끌기 전 상태로 돌아간다`() {
        val initial = setOf(40)

        dragSelectedIds(ids, initial, anchorId = 10, currentId = 50)
        val result = dragSelectedIds(ids, initial, anchorId = 10, currentId = 20)

        assertEquals(setOf(10, 20, 40), result)
    }

    @Test
    fun `목록에 없는 곡이면 처음 상태를 그대로 둔다`() {
        val result = dragSelectedIds(ids, initialCheckedIds = setOf(10), anchorId = 99, currentId = 20)

        assertEquals(setOf(10), result)
    }
}
