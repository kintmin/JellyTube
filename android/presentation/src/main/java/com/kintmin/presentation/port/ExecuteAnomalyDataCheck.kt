package com.kintmin.presentation.port

/** 데이터 이상 점검을 백그라운드 작업으로 넘긴다. */
interface ExecuteAnomalyDataCheck {

    /** 이미 실행 중이면 새로 넘기지 않고 false 를 돌려준다. */
    suspend operator fun invoke(): Boolean
}
