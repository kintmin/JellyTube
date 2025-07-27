package com.kintmin.log

/**
 *  제한1: params key의 최대 길이는 영문 기준 40자
 *  제한2: params value 최대 길이는 영문 기준 100자
 *  https://support.google.com/analytics/answer/9267744
 */
internal object FirebaseParam {

    private fun param(key: String, value: String): Pair<String, String> {
        return key.take(40) to value.take(100)
    }

    fun userId(value: String) = param("userId", value)
    fun errorMessage(exception: Throwable) = param("errorMessage", exception.message.toString())
    fun source(value: String) = param("source", value)
    fun availableRemMemory(value: Long?) = param("availableRemMemory", value.toString())
    fun isLowRemMemory(value: Boolean?) = param("isLowRemMemory", value.toString())
    fun availableStorage(value: Long?) = param("availableStorage", value.toString())
    fun isConnected(value: Boolean?) = param("isConnected", value.toString())
    fun isWifi(value: Boolean?) = param("isWifi", value.toString())
    fun isCellular(value: Boolean?) = param("isCellular", value.toString())
    fun downstreamKbps(value: Int?) = param("downstreamKbps", value.toString())
    fun upstreamKbps(value: Int?) = param("upstreamKbps", value.toString())
    fun audioMediaCount(value: Int) = param("audioMediaCount", value.toString())
    fun playlistId(value: Int) = param("playlistId", value.toString())
    fun playlistTitle(value: String) = param("playlistTitle", value)
}
