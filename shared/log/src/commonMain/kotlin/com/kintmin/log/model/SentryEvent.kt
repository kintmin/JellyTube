package com.kintmin.log.model

sealed class SentryEvent(val logName: SentryLogName, vararg val params: Pair<SentryParamName, String?>) {

    /** 값이 없는 파라미터는 싣지 않는다. 두 플랫폼이 이 결과를 그대로 보낸다. */
    val attributes: Map<String, String>
        get() = params.mapNotNull { (key, value) -> value?.let { key.name to it } }.toMap()

    data class SuccessRegisterUser(val userId: String) : SentryEvent(
        SentryLogName.SuccessRegisterUser,
        SentryParamName.UserId to userId
    )

    data class FailedRegisterUser(val exception: Throwable) : SentryEvent(
        SentryLogName.FailedRegisterUser,
        SentryParamName.ErrorMessage to exception.toString(),
    )

    data class FailedDownloadAudioMedia(
        val source: String,
        val exception: Throwable,
        val availableRemMemory: Long? = null,
        val isLowRemMemory: Boolean? = null,
        val availableStorage: Long? = null,
        val isConnected: Boolean? = null,
        val isWifi: Boolean? = null,
        val isCellular: Boolean? = null,
        val downstreamKbps: Int? = null,
        val upstreamKbps: Int? = null,
    ) : SentryEvent(
        SentryLogName.FailedDownloadAudioMedia,
        SentryParamName.Source to source,
        SentryParamName.ErrorMessage to exception.toString(),
        SentryParamName.AvailableRemMemory to availableRemMemory?.toString(),
        SentryParamName.IsLowRemMemory to isLowRemMemory?.toString(),
        SentryParamName.AvailableStorage to availableStorage?.toString(),
        SentryParamName.IsConnected to isConnected?.toString(),
        SentryParamName.IsWifi to isWifi?.toString(),
        SentryParamName.IsCellular to isCellular?.toString(),
        SentryParamName.DownstreamKbps to downstreamKbps?.toString(),
        SentryParamName.UpstreamKbps to upstreamKbps?.toString(),
    )

    data class AddAudioMedia(
        val source: String,
        val audioMediaCount: Int,
    ) : SentryEvent(
        SentryLogName.AddAudioMedia,
        SentryParamName.Source to source,
        SentryParamName.AudioMediaCount to audioMediaCount.toString(),
    )

    data class DeleteAudioMedia(
        val source: String,
    ) : SentryEvent(
        SentryLogName.DeleteAudioMedia,
        SentryParamName.Source to source,
    )

    data class AddPlaylist(
        val playlistId: Int,
        val playlistTitle: String,
    ) : SentryEvent(
        SentryLogName.AddPlaylist,
        SentryParamName.PlaylistId to playlistId.toString(),
        SentryParamName.PlaylistTitle to playlistTitle,
    )

    data class DeletePlaylist(
        val playlistId: Int,
        val playlistTitle: String,
        val audioMediaCount: Int,
    ) : SentryEvent(
        SentryLogName.DeletePlaylist,
        SentryParamName.PlaylistId to playlistId.toString(),
        SentryParamName.PlaylistTitle to playlistTitle,
        SentryParamName.AudioMediaCount to audioMediaCount.toString(),
    )

    /** 앱이 직접 받지 못하는 HLS 라 yt-dlp 가 앱 안에서 받은 경우. [reason] 이 지원하지 못한 형식이다. */
    data class YoutubeDownloadFallback(
        val source: String,
        val reason: String,
        val formatId: String,
        val protocol: String,
    ) : SentryEvent(
        SentryLogName.YoutubeDownloadFallback,
        SentryParamName.Source to source,
        SentryParamName.FallbackReason to reason,
        SentryParamName.FormatId to formatId,
        SentryParamName.Protocol to protocol,
    )
}
