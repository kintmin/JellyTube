package com.kintmin.log.model

/**
 * GA4 제약에 맞춰 자른 이벤트.
 * https://support.google.com/analytics/answer/9267744
 */
data class AnalyticsEvent(val name: String, val params: Map<String, String>)

/** 값이 없는 파라미터를 먼저 버린다. 버리기 전에 개수를 자르면 값 있는 파라미터가 밀려난다. */
fun FirebaseEvent.toAnalyticsEvent(): AnalyticsEvent = AnalyticsEvent(
    name = logName.name.take(MAX_ANALYTICS_NAME_LENGTH),
    params = params
        .mapNotNull { (key, value) -> value?.let { key.name.take(MAX_ANALYTICS_NAME_LENGTH) to it.take(MAX_ANALYTICS_VALUE_LENGTH) } }
        .take(MAX_ANALYTICS_PARAM_COUNT)
        .toMap(),
)

fun analyticsUserId(userId: String): String = userId.take(MAX_ANALYTICS_USER_ID_LENGTH)

private const val MAX_ANALYTICS_NAME_LENGTH = 40
private const val MAX_ANALYTICS_VALUE_LENGTH = 100
private const val MAX_ANALYTICS_PARAM_COUNT = 25
private const val MAX_ANALYTICS_USER_ID_LENGTH = 256
