package com.kintmin.presentation.ui.karaoke_search

sealed interface KaraokeSearchEvent {
    // 노래방 번호를 음원에 연동한 뒤 이전 화면으로 돌아간다.
    data object NavigateBack : KaraokeSearchEvent
}
