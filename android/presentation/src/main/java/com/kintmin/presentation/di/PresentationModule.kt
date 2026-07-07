package com.kintmin.presentation.di

import com.kintmin.presentation.ui.audio_media_detail.AudioMediaDetailViewModel
import com.kintmin.presentation.ui.audio_media_edit.AudioMediaEditViewModel
import com.kintmin.presentation.ui.main.MainViewModel
import com.kintmin.presentation.ui.main.floating_action.MainFloatingActionViewModel
import com.kintmin.presentation.ui.main.playlist.PlaylistViewModel
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadViewModel
import com.kintmin.presentation.ui.lyrics_detail.LyricsDetailViewModel
import com.kintmin.presentation.ui.lyrics_edit.LyricsEditViewModel
import com.kintmin.presentation.ui.lyrics_search.LyricsSearchViewModel
import com.kintmin.presentation.ui.lyrics_viewer.LyricsViewerViewModel
import com.kintmin.presentation.ui.player_bar.PlayerBarViewModel
import com.kintmin.presentation.ui.player_detail.PlayerDetailViewModel
import com.kintmin.presentation.ui.playlist_add.PlaylistAddViewModel
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderViewModel
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListViewModel
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListViewModel
import com.kintmin.presentation.ui.setting.SettingViewModel
import com.kintmin.presentation.ui.setting.app_log.AppLogViewModel
import com.kintmin.presentation.ui.setting.file_share_receive.SettingFileShareReceiveViewModel
import com.kintmin.presentation.ui.setting.quick_share.SettingShareViewModel
import com.kintmin.presentation.ui.step.StepViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule: Module = module {
    viewModelOf(::StepViewModel)
    viewModelOf(::SettingViewModel)
    viewModelOf(::PlayerBarViewModel)
    viewModelOf(::SettingFileShareReceiveViewModel)
    viewModelOf(::SettingShareViewModel)
    viewModelOf(::AppLogViewModel)
    viewModelOf(::PlayerDetailViewModel)
    viewModelOf(::MainFloatingActionViewModel)
    viewModelOf(::PlaylistViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::AudioMediaDetailViewModel)
    viewModelOf(::AudioMediaEditViewModel)
    viewModelOf(::LyricsSearchViewModel)
    viewModelOf(::LyricsDetailViewModel)
    viewModelOf(::LyricsEditViewModel)
    viewModelOf(::LyricsViewerViewModel)
    viewModelOf(::YoutubeDownloadViewModel)
    viewModelOf(::PlaylistDetailHeaderViewModel)
    viewModelOf(::PlaylistDetailListViewModel)
    viewModelOf(::PlaylistEditListViewModel)
    viewModelOf(::PlaylistAddViewModel)
}
