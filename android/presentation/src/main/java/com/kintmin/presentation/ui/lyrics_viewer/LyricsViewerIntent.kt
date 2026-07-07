package com.kintmin.presentation.ui.lyrics_viewer

sealed interface LyricsViewerIntent {
    // 재생 위치 폴링 갱신 (싱크 가사 하이라이트용)
    data object OnRefreshPosition : LyricsViewerIntent
}
