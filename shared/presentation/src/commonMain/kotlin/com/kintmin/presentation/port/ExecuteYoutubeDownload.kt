package com.kintmin.presentation.port

/** 유튜브 영상 다운로드를 백그라운드 작업으로 넘긴다. */
interface ExecuteYoutubeDownload {

    operator fun invoke(url: String)
}
