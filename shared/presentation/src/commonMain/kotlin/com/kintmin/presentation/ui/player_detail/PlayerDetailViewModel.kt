package com.kintmin.presentation.ui.player_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchSleepTimerAnnouncementOptionsFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchSleepTimerMinutesFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdateIsPlaybackShufflingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackPitchSemitoneUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackRepeatingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackSpeedUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdateSleepTimerAnnouncementMediaIdUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdateSleepTimerMinutesUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.audio_track.usecase.ToggleFavoriteUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.sleep_timer.SleepTimerRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PlayerDetailViewModel constructor(
    private val mediaControllerManager: PlaybackController,
    private val sleepTimerRunner: SleepTimerRunner,
    fetchIsPlaybackRepeatingFlowUseCase: FetchIsPlaybackRepeatingFlowUseCase,
    fetchIsPlaybackShufflingFlowUseCase: FetchIsPlaybackShufflingFlowUseCase,
    fetchPlaybackSpeedFlowUseCase: FetchPlaybackSpeedFlowUseCase,
    fetchPlaybackPitchSemitoneFlowUseCase: FetchPlaybackPitchSemitoneFlowUseCase,
    private val updatePlaybackRepeatingUseCase: UpdatePlaybackRepeatingUseCase,
    private val updateIsPlaybackShufflingUseCase: UpdateIsPlaybackShufflingUseCase,
    private val updatePlaybackSpeedUseCase: UpdatePlaybackSpeedUseCase,
    private val updatePlaybackPitchSemitoneUseCase: UpdatePlaybackPitchSemitoneUseCase,
    private val updateSleepTimerMinutesUseCase: UpdateSleepTimerMinutesUseCase,
    private val updateSleepTimerAnnouncementMediaIdUseCase: UpdateSleepTimerAnnouncementMediaIdUseCase,
    fetchSleepTimerMinutesFlowUseCase: FetchSleepTimerMinutesFlowUseCase,
    fetchSleepTimerAnnouncementOptionsFlowUseCase: FetchSleepTimerAnnouncementOptionsFlowUseCase,
    private val fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getParsedLyricsUseCase: GetParsedLyricsUseCase,
    private val getLyricsVariantUseCase: GetLyricsVariantUseCase,
) : ViewModel() {

    private val eventChannel = Channel<PlayerDetailEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlayerDetailEvent> = eventChannel.receiveAsFlow()

    private val _uiState = mediaControllerManager.playingMediaItem.let {
        MutableStateFlow(
            PlayerDetailUiState(
                id = it?.mediaId ?: "",
                playlistId = mediaControllerManager.currentPlaylistId,
                playlistName = "",
                title = it?.mediaTitle ?: "",
                artist = it?.mediaArtist ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (it?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = it?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
                isShuffling = false,
                isRepeating = false,
                playbackSpeed = mediaControllerManager.playbackSpeed,
                playbackPitchSemitone = mediaControllerManager.playbackPitchSemitone,
                repeatRangeStartDuration = mediaControllerManager.repeatRangeState.startDuration,
                repeatRangeEndDuration = mediaControllerManager.repeatRangeState.endDuration,
                sleepTimerRemaining = sleepTimerRunner.remaining,
            )
        )
    }
    val uiState: StateFlow<PlayerDetailUiState> = _uiState.asStateFlow()

    private var isHandlingSlider = false

    // 현재 곡의 파싱된 가사 줄(싱크 가사면 timeMs 포함). 재생 위치에 맞는 줄을 골라 표시한다.
    private var parsedLyricLines: List<LyricsLine> = emptyList()

    // 마지막으로 가사를 로드한 파일 경로. 곡이 바뀔 때만 재로드하기 위한 캐시 키.
    private var loadedLyricPath: String? = null

    init {
        refreshPlaylistName(mediaControllerManager.currentPlaylistId)
        observeAudioMediaDetail()
        viewModelScope.launch {
            fetchIsPlaybackRepeatingFlowUseCase().collect {
                _uiState.update { prev -> prev.copy(isRepeating = it) }
            }
        }
        viewModelScope.launch {
            fetchIsPlaybackShufflingFlowUseCase().collect {
                _uiState.update { prev -> prev.copy(isShuffling = it) }
            }
        }
        viewModelScope.launch {
            fetchPlaybackSpeedFlowUseCase().collect { speed ->
                _uiState.update { prev -> prev.copy(playbackSpeed = speed) }
            }
        }
        viewModelScope.launch {
            fetchPlaybackPitchSemitoneFlowUseCase().collect { semitone ->
                _uiState.update { prev -> prev.copy(playbackPitchSemitone = semitone) }
            }
        }
        viewModelScope.launch {
            fetchSleepTimerMinutesFlowUseCase().collect { minutes ->
                _uiState.update { prev -> prev.copy(sleepTimerMinutes = minutes) }
            }
        }
        viewModelScope.launch {
            fetchSleepTimerAnnouncementOptionsFlowUseCase().collect { options ->
                sleepTimerRunner.announcement = options.selected
                _uiState.update { prev -> prev.copy(sleepTimerAnnouncement = options) }
            }
        }
    }

    fun sendIntent(intent: PlayerDetailIntent) {
        when (intent) {
            PlayerDetailIntent.OnClickPlayOrPauseButton -> {
                if (mediaControllerManager.isPlaying) {
                    mediaControllerManager.pause()
                } else {
                    mediaControllerManager.resume()
                }
                _uiState.update {
                    it.copy(isPlaying = mediaControllerManager.isPlaying)
                }
            }

            PlayerDetailIntent.OnClickPreviousMediaButton -> {
                mediaControllerManager.clearRepeatRange()
                refreshRepeatRangeState()
                mediaControllerManager.playPrevious()
                refreshMediaData()
            }

            PlayerDetailIntent.OnClickNextMediaButton -> {
                mediaControllerManager.clearRepeatRange()
                refreshRepeatRangeState()
                mediaControllerManager.playNext()
                refreshMediaData()
            }
            
            PlayerDetailIntent.OnClickShuffleButton -> {
                viewModelScope.launch {
                    val newValue = !uiState.value.isShuffling
                    updateIsPlaybackShufflingUseCase(newValue)
                }
            }

            PlayerDetailIntent.OnClickRepeatButton -> {
                viewModelScope.launch {
                    val newValue = !uiState.value.isRepeating
                    updatePlaybackRepeatingUseCase(newValue)
                }
            }

            PlayerDetailIntent.OnClickRepeatRangeButton -> {
                mediaControllerManager.updateRepeatRange()
                refreshRepeatRangeState()
            }

            PlayerDetailIntent.OnClickFavoriteButton -> {
                val mediaId = uiState.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        eventChannel.send(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                        return@launch
                    }
                    toggleFavoriteUseCase(mediaId, !uiState.value.isFavorite)
                }
            }

            // 가사 입구는 셋이다 — 아트워크·한 줄 가사·부가 줄 아이콘. 셋 다 같은 일을 한다.
            // 아트워크는 가사 없는 곡에서 clickable 을 붙이지 않아 아래 분기로 오지 않는다.
            PlayerDetailIntent.OnClickLyrics -> {
                if (uiState.value.hasLyrics) {
                    _uiState.update { it.copy(isLyricsPanelVisible = !it.isLyricsPanelVisible) }
                    return
                }
                val mediaId = uiState.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        eventChannel.send(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        eventChannel.send(
                            PlayerDetailEvent.NavigateToLyricsSearch(
                                audioMediaId = mediaId,
                                query = uiState.value.title,
                                durationSeconds = uiState.value.playbackDuration.inWholeSeconds.toDouble(),
                            )
                        )
                    }
                }
            }

            PlayerDetailIntent.OnClickAddButton -> {
                val mediaId = uiState.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        eventChannel.send(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        eventChannel.send(PlayerDetailEvent.NavigateToAudioMediaInformationEditScreen(mediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickMoreButton -> {
                val mediaId = uiState.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        eventChannel.send(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        eventChannel.send(PlayerDetailEvent.NavigateToAudioMediaDetailScreen(mediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickPlayingPlaylistButton -> {
                viewModelScope.launch {
                    val playlistId = uiState.value.playlistId
                    val audioMediaId = uiState.value.id.toIntOrNull()
                    if (playlistId == null) {
                        eventChannel.send(PlayerDetailEvent.ShowToast("현재 재생 플레이리스트를 찾을 수 없습니다."))
                    } else {
                        eventChannel.send(PlayerDetailEvent.NavigateToPlayingPlaylist(playlistId, audioMediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickPlaybackSpeedButton -> {
                _uiState.update { it.copy(isPlaybackSpeedMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissPlaybackSpeedMenu -> {
                _uiState.update { it.copy(isPlaybackSpeedMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectPlaybackSpeed -> {
                viewModelScope.launch {
                    updatePlaybackSpeedUseCase(intent.speed)
                    _uiState.update {
                        it.copy(playbackSpeed = intent.speed)
                    }
                    mediaControllerManager.setPlaybackSpeed(intent.speed)
                }
            }

            PlayerDetailIntent.OnClickPlaybackPitchButton -> {
                _uiState.update { it.copy(isPlaybackPitchMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissPlaybackPitchMenu -> {
                _uiState.update { it.copy(isPlaybackPitchMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectPlaybackPitchSemitone -> {
                viewModelScope.launch {
                    updatePlaybackPitchSemitoneUseCase(intent.semitone)
                    _uiState.update {
                        it.copy(playbackPitchSemitone = intent.semitone)
                    }
                    mediaControllerManager.setPlaybackPitchSemitone(intent.semitone)
                }
            }

            is PlayerDetailIntent.OnChangeTimeSlider -> {
                isHandlingSlider = true
                _uiState.update {
                    it.copy(currentDuration = intent.duration.toDouble().seconds)
                }
            }

            PlayerDetailIntent.OnChangeFinishTimeSlider -> {
                isHandlingSlider = false
                mediaControllerManager.seek(uiState.value.currentDuration)
            }

            PlayerDetailIntent.OnClickSleepTimerButton -> {
                _uiState.update { it.copy(isSleepTimerMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissSleepTimerMenu -> {
                _uiState.update { it.copy(isSleepTimerMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectSleepTimerMinutes -> {
                viewModelScope.launch {
                    updateSleepTimerMinutesUseCase(intent.minutes)
                    _uiState.update { it.copy(sleepTimerMinutes = intent.minutes) }
                }
            }

            PlayerDetailIntent.OnStartSleepTimer -> {
                sleepTimerRunner.start(total = uiState.value.sleepTimerMinutes.minutes)
                _uiState.update {
                    it.copy(
                        sleepTimerRemaining = sleepTimerRunner.remaining,
                        isSleepTimerMenuVisible = false,
                    )
                }
            }

            PlayerDetailIntent.OnCancelSleepTimer -> {
                sleepTimerRunner.cancel()
                _uiState.update {
                    it.copy(
                        sleepTimerRemaining = sleepTimerRunner.remaining,
                        isSleepTimerMenuVisible = false,
                    )
                }
            }

            PlayerDetailIntent.OnClickSleepTimerAnnouncementButton -> {
                _uiState.update { it.copy(isSleepTimerAnnouncementMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissSleepTimerAnnouncementMenu -> {
                _uiState.update { it.copy(isSleepTimerAnnouncementMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectSleepTimerAnnouncement -> {
                viewModelScope.launch {
                    updateSleepTimerAnnouncementMediaIdUseCase(intent.mediaId)
                    _uiState.update { it.copy(isSleepTimerAnnouncementMenuVisible = false) }
                }
            }

            PlayerDetailIntent.OnRefreshMediaData -> refreshMediaData()
        }
    }

    private fun refreshMediaData() {
        if (isHandlingSlider) return

        mediaControllerManager.repeatRangeIfNeeded()
        val currentDuration = mediaControllerManager.currentPosition?.toDuration(DurationUnit.MILLISECONDS) ?: return
        val currentPlayingItem = mediaControllerManager.playingMediaItem
        val currentPlaylistId = mediaControllerManager.currentPlaylistId
        val isSamePlaylist = uiState.value.playlistId == currentPlaylistId
        val isSameMedia = uiState.value.id == currentPlayingItem?.mediaId

        val repeatRangeState = mediaControllerManager.repeatRangeState
        if (isSameMedia && isSamePlaylist) {
            _uiState.update {
                it.copy(
                    currentDuration = currentDuration,
                    isPlaying = mediaControllerManager.isPlaying,
                    repeatRangeStartDuration = repeatRangeState.startDuration,
                    repeatRangeEndDuration = repeatRangeState.endDuration,
                    sleepTimerRemaining = sleepTimerRunner.remaining,
                )
            }
            refreshActiveLyricIndex()
            return
        }

        _uiState.update {
            PlayerDetailUiState(
                id = currentPlayingItem?.mediaId ?: "",
                playlistId = currentPlaylistId,
                playlistName = if (isSamePlaylist) uiState.value.playlistName else "",
                title = currentPlayingItem?.mediaTitle ?: "",
                artist = currentPlayingItem?.mediaArtist ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (currentPlayingItem?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = currentPlayingItem?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
                isShuffling = uiState.value.isShuffling,
                isRepeating = uiState.value.isRepeating,
                playbackSpeed = uiState.value.playbackSpeed,
                playbackPitchSemitone = uiState.value.playbackPitchSemitone,
                // 같은 곡이면 즐겨찾기 상태 유지, 곡이 바뀌면 observeFavorite의 flow가 새 값으로 갱신
                isFavorite = if (isSameMedia) uiState.value.isFavorite else false,
                // 같은 곡인데 플레이리스트만 바뀌면 구독은 id 로 묶여 다시 흐르지 않는다. 그때 가사
                // 상태를 버리면 아트워크가 눌리지 않게 되므로 이어 받는다.
                hasLyrics = if (isSameMedia) uiState.value.hasLyrics else false,
                lyricLines = if (isSameMedia) uiState.value.lyricLines else emptyList(),
                isLyricsPanelVisible = uiState.value.isLyricsPanelVisible,
                repeatRangeStartDuration = repeatRangeState.startDuration,
                repeatRangeEndDuration = repeatRangeState.endDuration,
                isPlaybackSpeedMenuVisible = uiState.value.isPlaybackSpeedMenuVisible,
                isPlaybackPitchMenuVisible = uiState.value.isPlaybackPitchMenuVisible,
                sleepTimerRemaining = sleepTimerRunner.remaining,
                sleepTimerMinutes = uiState.value.sleepTimerMinutes,
                sleepTimerAnnouncement = uiState.value.sleepTimerAnnouncement,
                isSleepTimerMenuVisible = uiState.value.isSleepTimerMenuVisible,
                isSleepTimerAnnouncementMenuVisible = uiState.value.isSleepTimerAnnouncementMenuVisible,
            )
        }

        refreshActiveLyricIndex()

        if (!isSamePlaylist) {
            refreshPlaylistName(currentPlaylistId)
        }
    }

    // 현재 재생 곡(uiState.id)이 바뀔 때마다 즐겨찾기/가사/표기 상태를 재구독한다.
    // flatMapLatest가 이전 곡의 구독을 자동 취소하므로 별도 Job 관리가 필요 없다.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAudioMediaDetail() {
        viewModelScope.launch {
            uiState.map { it.id.toIntOrNull() }
                .distinctUntilChanged()
                .flatMapLatest { mediaId ->
                    if (mediaId == null) {
                        flowOf(null)
                    } else {
                        fetchAudioMediaDetailFlowUseCase(mediaId)
                    }
                }
                .collect { aggregates ->
                    val isFavorite = aggregates?.any { it.playlist.type == PlaylistType.FAVORITE } ?: false
                    val audioMedia = aggregates?.firstOrNull()?.audioMedia
                    val lyricFileFullPath = audioMedia?.lyricFileFullPath
                    _uiState.update {
                        it.copy(
                            isFavorite = isFavorite,
                            hasLyrics = lyricFileFullPath != null,
                            // 제목·아티스트는 저장소가 진실이다. 재생기가 들고 있는 값은 곡을 걸 때
                            // 찍힌 스냅샷이라, 정보를 고쳐도 다시 걸기 전까지 옛 이름이 남았다.
                            // 어느 플레이리스트에도 없어 목록이 비면 덮지 않고 그대로 둔다.
                            title = audioMedia?.name ?: it.title,
                            artist = audioMedia?.artist ?: it.artist,
                        )
                    }
                    loadLyricsIfNeeded(lyricFileFullPath)
                }
        }
    }

    // 곡이 바뀌어 가사 파일 경로가 달라졌을 때만 가사를 로드/파싱한다.
    private fun loadLyricsIfNeeded(lyricFileFullPath: String?) {
        if (lyricFileFullPath == loadedLyricPath) return
        loadedLyricPath = lyricFileFullPath

        if (lyricFileFullPath == null) {
            parsedLyricLines = emptyList()
            _uiState.update { it.copy(lyricLines = emptyList(), activeLyricIndex = -1) }
            return
        }
        viewModelScope.launch {
            parsedLyricLines = getParsedLyricsUseCase(lyricFileFullPath)
            // 음차·번역 파일은 원본과 같은 타임코드로 만들어져 줄 번호가 맞는다
            val translationLines = getLyricsVariantUseCase(lyricFileFullPath, LyricsVariant.TRANSLATION).getOrNull()
            val transliterationLines = getLyricsVariantUseCase(lyricFileFullPath, LyricsVariant.TRANSLITERATION).getOrNull()
            _uiState.update {
                it.copy(
                    lyricLines = parsedLyricLines.mapIndexed { index, line ->
                        PlayerLyricLineUiState(
                            text = line.text,
                            transliteration = transliterationLines?.getOrNull(index)?.text?.takeIf(String::isNotBlank),
                            translation = translationLines?.getOrNull(index)?.text?.takeIf(String::isNotBlank),
                        )
                    },
                    activeLyricIndex = -1,
                )
            }
            refreshActiveLyricIndex()
        }
    }

    // 비싱크(시간정보 없음) 가사면 activeLyricIndex 가 늘 -1 이라 활성 줄이 없다.
    private fun refreshActiveLyricIndex() {
        val index = if (parsedLyricLines.isEmpty()) {
            -1
        } else {
            val positionMs = mediaControllerManager.currentPosition ?: return
            activeLyricIndex(parsedLyricLines, positionMs)
        }
        if (index != _uiState.value.activeLyricIndex) {
            _uiState.update { it.copy(activeLyricIndex = index) }
        }
    }

    private fun refreshRepeatRangeState() {
        val repeatRangeState = mediaControllerManager.repeatRangeState
        _uiState.update {
            it.copy(
                repeatRangeStartDuration = repeatRangeState.startDuration,
                repeatRangeEndDuration = repeatRangeState.endDuration,
            )
        }
    }

    private fun refreshPlaylistName(playlistId: Int?) {
        if (playlistId == null) {
            _uiState.update { it.copy(playlistName = "") }
            return
        }
        viewModelScope.launch {
            val name = runCatching {
                fetchPlaylistFlowUseCase(playlistId).first().name
            }.getOrDefault("")
            _uiState.update { it.copy(playlistName = name) }
        }
    }
}

