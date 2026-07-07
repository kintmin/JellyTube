package com.kintmin.presentation.ui.player_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdateIsPlaybackShufflingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackPitchSemitoneUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackRepeatingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackSpeedUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.audio_track.usecase.ToggleFavoriteUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.GetAudioMediaLyricsUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.platform.service_controller.MediaControllerManager
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PlayerDetailViewModel constructor(
    private val mediaControllerManager: MediaControllerManager,
    fetchIsPlaybackRepeatingFlowUseCase: FetchIsPlaybackRepeatingFlowUseCase,
    fetchIsPlaybackShufflingFlowUseCase: FetchIsPlaybackShufflingFlowUseCase,
    fetchPlaybackSpeedFlowUseCase: FetchPlaybackSpeedFlowUseCase,
    fetchPlaybackPitchSemitoneFlowUseCase: FetchPlaybackPitchSemitoneFlowUseCase,
    private val updatePlaybackRepeatingUseCase: UpdatePlaybackRepeatingUseCase,
    private val updateIsPlaybackShufflingUseCase: UpdateIsPlaybackShufflingUseCase,
    private val updatePlaybackSpeedUseCase: UpdatePlaybackSpeedUseCase,
    private val updatePlaybackPitchSemitoneUseCase: UpdatePlaybackPitchSemitoneUseCase,
    private val fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getAudioMediaLyricsUseCase: GetAudioMediaLyricsUseCase,
    private val parseLyricsUseCase: ParseLyricsUseCase,
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
                mediaControllerManager.setRepeatMode(it)
            }
        }
        viewModelScope.launch {
            fetchIsPlaybackShufflingFlowUseCase().collect {
                _data.update { prev -> prev.copy(isShuffling = it) }
                mediaControllerManager.setShuffleMode(it)
            }
        }
        viewModelScope.launch {
            fetchPlaybackSpeedFlowUseCase().collect { speed ->
                _data.update { prev -> prev.copy(playbackSpeed = speed) }
                mediaControllerManager.setPlaybackSpeed(speed)
            }
        }
        viewModelScope.launch {
            fetchPlaybackPitchSemitoneFlowUseCase().collect { semitone ->
                _data.update { prev -> prev.copy(playbackPitchSemitone = semitone) }
                mediaControllerManager.setPlaybackPitchSemitone(semitone)
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

            PlayerDetailIntent.OnClickLyricsButton -> {
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToLyricsViewer(mediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickAddButton -> {
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToAudioMediaEditScreen(mediaId))
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
                    it.copy(currentDuration = intent.duration.toLong().seconds)
                }
            }

            PlayerDetailIntent.OnChangeFinishTimeSlider -> {
                isHandlingSlider = false
                mediaControllerManager.seek(data.value.currentDuration)
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
                )
            }
            refreshLyricLine()
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
                repeatRangeStartDuration = repeatRangeState.startDuration,
                repeatRangeEndDuration = repeatRangeState.endDuration,
                isPlaybackSpeedMenuVisible = data.value.isPlaybackSpeedMenuVisible,
                isPlaybackPitchMenuVisible = data.value.isPlaybackPitchMenuVisible,
            )
        }

        refreshLyricLine()

        if (!isSamePlaylist) {
            refreshPlaylistName(currentPlaylistId)
        }
    }

    // 현재 재생 곡(data.id)이 바뀔 때마다 즐겨찾기/가사 유무 상태를 재구독한다.
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
                    val lyricFileFullPath = aggregates?.firstOrNull()?.audioMedia?.lyricFileFullPath
                    _data.update { it.copy(isFavorite = isFavorite, hasLyrics = lyricFileFullPath != null) }
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
            _data.update { it.copy(currentLyricLine = "") }
            return
        }
        viewModelScope.launch {
            val rawLyrics = getAudioMediaLyricsUseCase(lyricFileFullPath).getOrNull().orEmpty()
            parsedLyricLines = parseLyricsUseCase(rawLyrics)
            refreshLyricLine()
        }
    }

    // 현재 재생 위치에 해당하는 싱크 가사 한 줄을 UI 상태에 반영한다.
    // 비싱크(시간정보 없음) 가사면 activeLyricIndex 가 -1 이라 빈 문자열로 둔다.
    private fun refreshLyricLine() {
        val line = if (parsedLyricLines.isEmpty()) {
            ""
        } else {
            val positionMs = mediaControllerManager.currentPosition ?: return
            val index = activeLyricIndex(parsedLyricLines, positionMs)
            parsedLyricLines.getOrNull(index)?.text.orEmpty()
        }
        if (line != _data.value.currentLyricLine) {
            _data.update { it.copy(currentLyricLine = line) }
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

