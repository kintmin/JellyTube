package com.kintmin.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals

class IcloudPlaceholderNameTest {

    @Test
    fun `내려받지 않은 항목의 자리 이름을 원래 이름으로 되돌린다`() {
        assertEquals(
            "JellyTube_20261127_9f1c2d3e.json.gz",
            resolvedIcloudName(".JellyTube_20261127_9f1c2d3e.json.gz.icloud"),
        )
        assertEquals("jellytube-backup.json.gz", resolvedIcloudName(".jellytube-backup.json.gz.icloud"))
    }

    @Test
    fun `이미 내려받은 이름은 그대로 둔다`() {
        assertEquals("JellyTube_20261127_9f1c2d3e.json.gz", resolvedIcloudName("JellyTube_20261127_9f1c2d3e.json.gz"))
    }

    @Test
    fun `점으로 시작하거나 끝이 같기만 한 이름은 건드리지 않는다`() {
        assertEquals(".hidden", resolvedIcloudName(".hidden"))
        assertEquals("backup.icloud", resolvedIcloudName("backup.icloud"))
        assertEquals(".icloud", resolvedIcloudName(".icloud"))
    }
}
