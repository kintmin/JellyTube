package com.kintmin.log.model

/**
 * param key 제한: 최대 길이는 영문 기준 40자
 * https://support.google.com/analytics/answer/9267744
 */
enum class FirebaseParamName {
    UserId,
    ErrorMessage,
    Source,
    AvailableRemMemory,
    IsLowRemMemory,
    AvailableStorage,
    IsConnected,
    IsWifi,
    IsCellular,
    DownstreamKbps,
    UpstreamKbps,
    AudioMediaCount,
    PlaylistId,
    PlaylistTitle,
}
