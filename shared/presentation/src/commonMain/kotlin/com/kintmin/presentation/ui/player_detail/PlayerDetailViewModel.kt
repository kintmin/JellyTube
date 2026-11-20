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
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.sleep_timer.SleepTimerRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<PlayerDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _data = mediaControllerManager.playingMediaItem.let {
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
    val data = _data.asStateFlow()

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
                _data.update { prev -> prev.copy(isRepeating = it) }
            }
        }
        viewModelScope.launch {
            fetchIsPlaybackShufflingFlowUseCase().collect {
                _data.update { prev -> prev.copy(isShuffling = it) }
            }
        }
        viewModelScope.launch {
            fetchPlaybackSpeedFlowUseCase().collect { speed ->
                _data.update { prev -> prev.copy(playbackSpeed = speed) }
            }
        }
        viewModelScope.launch {
            fetchPlaybackPitchSemitoneFlowUseCase().collect { semitone ->
                _data.update { prev -> prev.copy(playbackPitchSemitone = semitone) }
            }
        }
        viewModelScope.launch {
            fetchSleepTimerMinutesFlowUseCase().collect { minutes ->
                _data.update { prev -> prev.copy(sleepTimerMinutes = minutes) }
            }
        }
        viewModelScope.launch {
            fetchSleepTimerAnnouncementOptionsFlowUseCase().collect { options ->
                sleepTimerRunner.announcement = options.selected
                _data.update { prev -> prev.copy(sleepTimerAnnouncement = options) }
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
                _data.update {
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
                    val newValue = !data.value.isShuffling
                    updateIsPlaybackShufflingUseCase(newValue)
                }
            }

            PlayerDetailIntent.OnClickRepeatButton -> {
                viewModelScope.launch {
                    val newValue = !data.value.isRepeating
                    updatePlaybackRepeatingUseCase(newValue)
                }
            }

            PlayerDetailIntent.OnClickRepeatRangeButton -> {
                viewModelScope.launch {
                    mediaControllerManager.updateRepeatRange()
                        .onSuccess { refreshRepeatRangeState() }
                        .onFailure { _eventFlow.emit(PlayerDetailEvent.ShowToast("B 지점은 A 지점 이후로 설정해주세요.")) }
                }
            }

            PlayerDetailIntent.OnClickFavoriteButton -> {
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                        return@launch
                    }
                    toggleFavoriteUseCase(mediaId, !data.value.isFavorite)
                }
            }

            // 가사 입구는 셋이다 — 아트워크·한 줄 가사·부가 줄 아이콘. 셋 다 같은 일을 한다.
            // 아트워크는 가사 없는 곡에서 clickable 을 붙이지 않아 아래 분기로 오지 않는다.
            PlayerDetailIntent.OnClickLyrics -> {
                if (data.value.hasLyrics) {
                    _data.update { it.copy(isLyricsPanelVisible = !it.isLyricsPanelVisible) }
                    return
                }
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        _eventFlow.emit(
                            PlayerDetailEvent.NavigateToLyricsSearch(
                                audioMediaId = mediaId,
                                query = data.value.title,
                                durationSeconds = data.value.playbackDuration.inWholeSeconds.toDouble(),
                            )
                        )
                    }
                }
            }

            PlayerDetailIntent.OnClickAddButton -> {
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToAudioMediaInformationEditScreen(mediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickMoreButton -> {
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToAudioMediaDetailScreen(mediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickPlayingPlaylistButton -> {
                viewModelScope.launch {
                    val playlistId = data.value.playlistId
                    val audioMediaId = data.value.id.toIntOrNull()
                    if (playlistId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 재생 플레이리스트를 찾을 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToPlayingPlaylist(playlistId, audioMediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickPlaybackSpeedButton -> {
                _data.update { it.copy(isPlaybackSpeedMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissPlaybackSpeedMenu -> {
                _data.update { it.copy(isPlaybackSpeedMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectPlaybackSpeed -> {
                viewModelScope.launch {
                    updatePlaybackSpeedUseCase(intent.speed)
                    _data.update {
                        it.copy(playbackSpeed = intent.speed)
                    }
                    mediaControllerManager.setPlaybackSpeed(intent.speed)
                }
            }

            PlayerDetailIntent.OnClickPlaybackPitchButton -> {
                _data.update { it.copy(isPlaybackPitchMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissPlaybackPitchMenu -> {
                _data.update { it.copy(isPlaybackPitchMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectPlaybackPitchSemitone -> {
                viewModelScope.launch {
                    updatePlaybackPitchSemitoneUseCase(intent.semitone)
                    _data.update {
                        it.copy(playbackPitchSemitone = intent.semitone)
                    }
                    mediaControllerManager.setPlaybackPitchSemitone(intent.semitone)
                }
            }

            is PlayerDetailIntent.OnChangeTimeSlider -> {
                isHandlingSlider = true
                _data.update {
                    it.copy(currentDuration = intent.duration.toDouble().seconds)
                }
            }

            PlayerDetailIntent.OnChangeFinishTimeSlider -> {
                isHandlingSlider = false
                mediaControllerManager.seek(data.value.currentDuration)
            }

            PlayerDetailIntent.OnClickSleepTimerButton -> {
                _data.update { it.copy(isSleepTimerMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissSleepTimerMenu -> {
                _data.update { it.copy(isSleepTimerMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectSleepTimerMinutes -> {
                viewModelScope.launch {
                    updateSleepTimerMinutesUseCase(intent.minutes)
                    _data.update { it.copy(sleepTimerMinutes = intent.minutes) }
                }
            }

            PlayerDetailIntent.OnStartSleepTimer -> {
                sleepTimerRunner.start(total = data.value.sleepTimerMinutes.minutes)
                _data.update {
                    it.copy(
                        sleepTimerRemaining = sleepTimerRunner.remaining,
                        isSleepTimerMenuVisible = false,
                    )
                }
            }

            PlayerDetailIntent.OnCancelSleepTimer -> {
                sleepTimerRunner.cancel()
                _data.update {
                    it.copy(
                        sleepTimerRemaining = sleepTimerRunner.remaining,
                        isSleepTimerMenuVisible = false,
                    )
                }
            }

            PlayerDetailIntent.OnClickSleepTimerAnnouncementButton -> {
                _data.update { it.copy(isSleepTimerAnnouncementMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissSleepTimerAnnouncementMenu -> {
                _data.update { it.copy(isSleepTimerAnnouncementMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectSleepTimerAnnouncement -> {
                viewModelScope.launch {
                    updateSleepTimerAnnouncementMediaIdUseCase(intent.mediaId)
                    _data.update { it.copy(isSleepTimerAnnouncementMenuVisible = false) }
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
        val isSamePlaylist = data.value.playlistId == currentPlaylistId
        val isSameMedia = data.value.id == currentPlayingItem?.mediaId

        val repeatRangeState = mediaControllerManager.repeatRangeState
        if (isSameMedia && isSamePlaylist) {
            _data.update {
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

        _data.update {
            PlayerDetailUiState(
                id = currentPlayingItem?.mediaId ?: "",
                playlistId = currentPlaylistId,
                playlistName = if (isSamePlaylist) data.value.playlistName else "",
                title = currentPlayingItem?.mediaTitle ?: "",
                artist = currentPlayingItem?.mediaArtist ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (currentPlayingItem?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = currentPlayingItem?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
                isShuffling = data.value.isShuffling,
                isRepeating = data.value.isRepeating,
                playbackSpeed = data.value.playbackSpeed,
                playbackPitchSemitone = data.value.playbackPitchSemitone,
                // 같은 곡이면 즐겨찾기 상태 유지, 곡이 바뀌면 observeFavorite의 flow가 새 값으로 갱신
                isFavorite = if (isSameMedia) data.value.isFavorite else false,
                // 같은 곡인데 플레이리스트만 바뀌면 구독은 id 로 묶여 다시 흐르지 않는다. 그때 가사
                // 상태를 버리면 아트워크가 눌리지 않게 되므로 이어 받는다.
                hasLyrics = if (isSameMedia) data.value.hasLyrics else false,
                lyricLines = if (isSameMedia) data.value.lyricLines else emptyList(),
                isLyricsPanelVisible = data.value.isLyricsPanelVisible,
                repeatRangeStartDuration = repeatRangeState.startDuration,
                repeatRangeEndDuration = repeatRangeState.endDuration,
                isPlaybackSpeedMenuVisible = data.value.isPlaybackSpeedMenuVisible,
                isPlaybackPitchMenuVisible = data.value.isPlaybackPitchMenuVisible,
                sleepTimerRemaining = sleepTimerRunner.remaining,
                sleepTimerMinutes = data.value.sleepTimerMinutes,
                sleepTimerAnnouncement = data.value.sleepTimerAnnouncement,
                isSleepTimerMenuVisible = data.value.isSleepTimerMenuVisible,
                isSleepTimerAnnouncementMenuVisible = data.value.isSleepTimerAnnouncementMenuVisible,
            )
        }

        refreshActiveLyricIndex()

        if (!isSamePlaylist) {
            refreshPlaylistName(currentPlaylistId)
        }
    }

    // 현재 재생 곡(data.id)이 바뀔 때마다 즐겨찾기/가사/표기 상태를 재구독한다.
    // flatMapLatest가 이전 곡의 구독을 자동 취소하므로 별도 Job 관리가 필요 없다.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAudioMediaDetail() {
        viewModelScope.launch {
            data.map { it.id.toIntOrNull() }
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
                    _data.update {
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
            _data.update { it.copy(lyricLines = emptyList(), activeLyricIndex = -1) }
            return
        }
        viewModelScope.launch {
            parsedLyricLines = getParsedLyricsUseCase(lyricFileFullPath)
            _data.update {
                it.copy(
                    lyricLines = parsedLyricLines.map { line -> line.text },
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
        if (index != _data.value.activeLyricIndex) {
            _data.update { it.copy(activeLyricIndex = index) }
        }
    }

    private fun refreshRepeatRangeState() {
        val repeatRangeState = mediaControllerManager.repeatRangeState
        _data.update {
            it.copy(
                repeatRangeStartDuration = repeatRangeState.startDuration,
                repeatRangeEndDuration = repeatRangeState.endDuration,
            )
        }
    }

    private fun refreshPlaylistName(playlistId: Int?) {
        if (playlistId == null) {
            _data.update { it.copy(playlistName = "") }
            return
        }
        viewModelScope.launch {
            val name = runCatching {
                fetchPlaylistFlowUseCase(playlistId).first().name
            }.getOrDefault("")
            _data.update { it.copy(playlistName = name) }
        }
    }
}

