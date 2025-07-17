package com.kintmin.log

/**
 * 제한1: 이벤트명의 최대 길이는 영문 기준 40자
 * 제한2: 한 로그 당 params 최대 개수는 25개
 * 제한3: params key의 최대 길이는 영문 기준 40자
 * 제한4: params value 최대 길이는 영문 기준 100자
 * https://support.google.com/analytics/answer/9267744
 */
sealed class FirebaseEvent(val logName: String, vararg val params: Pair<String, Any?>) {

}