package com.kintmin.log.model

/**
 * 이벤트 개수 제한: 앱 사용자당 500개
 * 이벤트명 제한: 최대 길이는 영문 기준 40자
 * https://support.google.com/analytics/answer/9267744
 */
enum class FirebaseLogName {
    SuccessRegisterUser,
    FailedRegisterUser,
    FailedDownloadAudioMedia,
    AddAudioMedia,
    DeleteAudioMedia,
    AddPlaylist,
    DeletePlaylist,
}