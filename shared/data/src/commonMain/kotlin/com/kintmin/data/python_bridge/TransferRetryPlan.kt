package com.kintmin.data.python_bridge

/** 유튜브 전송의 구간·조각 하나를 같은 주소로 다시 받을지. 주소가 상한 403·404·410 은 재추출이 맡는다. */
object TransferRetryPlan {

    private val DELAY_SECONDS = listOf(1.0, 3.0)

    fun isTransient(statusCode: Int): Boolean = statusCode == 429 || statusCode in 500..599

    /** [attempt] 번째(1부터) 재시도 전에 기다릴 초. 한도를 넘으면 null. */
    fun delaySeconds(attempt: Int): Double? = DELAY_SECONDS.getOrNull(attempt - 1)
}
