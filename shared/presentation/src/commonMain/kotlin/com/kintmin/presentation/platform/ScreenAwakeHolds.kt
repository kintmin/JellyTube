package com.kintmin.presentation.platform

/**
 * 화면 꺼짐을 붙잡은 쪽이 여럿일 때 **먼저 놓은 쪽이 남의 것까지 풀어 버리는 것**을 막는다.
 *
 * 실제로 켜고 끄는 일은 부르는 쪽이 한다 — 무엇을 하나로 쥐고 있는지가 플랫폼마다 다르다.
 */
internal class ScreenAwakeHolds {
    private var count = 0

    /** 처음 붙잡은 것이면 true. 그때만 실제로 켠다. */
    fun acquire(): Boolean = count++ == 0

    /** 마지막으로 놓은 것이면 true. 그때만 실제로 끈다. */
    fun release(): Boolean {
        if (count == 0) return false
        return --count == 0
    }
}
