package com.kintmin.log.model

sealed class FirebaseEvent(val logName: FirebaseLogName, vararg val params: Pair<FirebaseParamName, String?>) {

    data class SuccessRegisterUser(val userId: String) : FirebaseEvent(
        FirebaseLogName.SuccessRegisterUser,
        FirebaseParamName.UserId to userId
    )

    data class FailedRegisterUser(val exception: Throwable) : FirebaseEvent(
        FirebaseLogName.FailedRegisterUser,
        FirebaseParamName.ErrorMessage to exception.toString(),
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
    ) : FirebaseEvent(
        FirebaseLogName.FailedDownloadAudioMedia,
        FirebaseParamName.Source to source,
        FirebaseParamName.ErrorMessage to exception.toString(),
        FirebaseParamName.AvailableRemMemory to availableRemMemory?.toString(),
        FirebaseParamName.IsLowRemMemory to isLowRemMemory?.toString(),
        FirebaseParamName.AvailableStorage to availableStorage?.toString(),
        FirebaseParamName.IsConnected to isConnected?.toString(),
        FirebaseParamName.IsWifi to isWifi?.toString(),
        FirebaseParamName.IsCellular to isCellular?.toString(),
        FirebaseParamName.DownstreamKbps to downstreamKbps?.toString(),
        FirebaseParamName.UpstreamKbps to upstreamKbps?.toString(),
    )

    data class AddAudioMedia(
        val source: String,
        val audioMediaCount: Int,
    ) : FirebaseEvent(
        FirebaseLogName.AddAudioMedia,
        FirebaseParamName.Source to source,
        FirebaseParamName.AudioMediaCount to audioMediaCount.toString(),
    )

    data class DeleteAudioMedia(
        val source: String,
    ) : FirebaseEvent(
        FirebaseLogName.DeleteAudioMedia,
        FirebaseParamName.Source to source,
    )

    data class AddPlaylist(
        val playlistId: Int,
        val playlistTitle: String,
    ) : FirebaseEvent(
        FirebaseLogName.AddPlaylist,
        FirebaseParamName.PlaylistId to playlistId.toString(),
        FirebaseParamName.PlaylistTitle to playlistTitle,
    )

    data class DeletePlaylist(
        val playlistId: Int,
        val playlistTitle: String,
        val audioMediaCount: Int,
    ) : FirebaseEvent(
        FirebaseLogName.DeletePlaylist,
        FirebaseParamName.PlaylistId to playlistId.toString(),
        FirebaseParamName.PlaylistTitle to playlistTitle,
        FirebaseParamName.AudioMediaCount to audioMediaCount.toString(),
    )

    /** 앱이 직접 받지 못하는 HLS 라 yt-dlp 가 앱 안에서 받은 경우. [reason] 이 지원하지 못한 형식이다. */
    data class YoutubeDownloadFallback(
        val source: String,
        val reason: String,
        val formatId: String,
        val protocol: String,
    ) : FirebaseEvent(
        FirebaseLogName.YoutubeDownloadFallback,
        FirebaseParamName.Source to source,
        FirebaseParamName.FallbackReason to reason,
        FirebaseParamName.FormatId to formatId,
        FirebaseParamName.Protocol to protocol,
    )
}
