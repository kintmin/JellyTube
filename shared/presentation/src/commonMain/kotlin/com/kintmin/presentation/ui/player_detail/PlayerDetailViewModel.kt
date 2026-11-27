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
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.audio_track.usecase.ToggleFavoriteUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetPlaybackLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.GetPlaylistUseCase
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.sleep_timer.SleepTimerRunner
import com.kintmin.presentation.util.NextLyricLineTimer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val getPlaylistUseCase: GetPlaylistUseCase,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getPlaybackLyricsUseCase: GetPlaybackLyricsUseCase,
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
                hasNext = mediaControllerManager.hasNext,
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

    private var parsedLyricLines: List<LyricsLine> = emptyList()

    // 마지막으로 가사를 로드한 파일 경로. 곡이 바뀔 때만 재로드하기 위한 캐시 키.
    private var loadedLyricPath: String? = null
    private var loadedLyricsEdited = false

    private val nextLyricLineTimer = NextLyricLineTimer(viewModelScope)

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
                // 곡 처음으로만 돌아갈 수 있어 구간을 직접 지우지 않는다. 곡이 바뀌면 구간이 스스로 풀린다.
                mediaControllerManager.playPrevious()
                refreshRepeatRangeState()
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

            PlayerDetailIntent.OnClickLyrics -> {
                _uiState.update {
                    when {
                        // 닫는 뜻은 가사 유무와 무관하게 분명하다.
                        it.showsLyricsFace ->
                            it.copy(isLyricsPanelVisible = false, isLyricsMissingNoticeVisible = false)
                        // 이 곡에 가사가 있는지 아직 모른다. 이전 곡의 답으로 열면 틀린 면이 뜨고,
                        // 그때 켜진 선호가 이후 곡까지 따라간다. 알게 되면 그때 연다.
                        !it.isLyricsKnown -> it.copy(isLyricsFaceRequested = true)
                        it.hasLyrics -> it.copy(isLyricsPanelVisible = true)
                        else -> it.copy(isLyricsMissingNoticeVisible = true)
                    }
                }
            }

            PlayerDetailIntent.OnClickLyricsSearchButton -> {
                val mediaId = uiState.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        eventChannel.send(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                        return@launch
                    }
                    // 찾겠다는 것은 찾으면 보겠다는 뜻이다. 저장하고 돌아왔을 때 가사 면이 바로 뜬다.
                    _uiState.update { it.copy(isLyricsPanelVisible = true) }
                    eventChannel.send(
                        PlayerDetailEvent.NavigateToLyricsSearch(
                            audioMediaId = mediaId,
                            query = uiState.value.title,
                            durationSeconds = uiState.value.playbackDuration.inWholeSeconds.toDouble(),
                            playbackDurationSeconds = uiState.value.playbackDuration.inWholeSeconds.toDouble(),
                        )
                    )
                }
            }

            is PlayerDetailIntent.OnClickLyricLine -> {
                // 새 가사를 읽기 전까지는 이전 곡 가사가 떠 있다. 그 줄의 타임코드로 새 곡을 옮기지 않는다.
                if (!uiState.value.isLyricsKnown) return
                val timeMs = parsedLyricLines.getOrNull(intent.index)?.timeMs ?: return
                mediaControllerManager.seek(timeMs.milliseconds)
                refreshActiveLyricIndex()
            }

            PlayerDetailIntent.OnClickLyricsViewerButton -> {
                val mediaId = uiState.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        eventChannel.send(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        eventChannel.send(PlayerDetailEvent.NavigateToLyricsViewer(mediaId))
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
                refreshActiveLyricIndex()
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
                    hasNext = mediaControllerManager.hasNext,
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
                hasNext = mediaControllerManager.hasNext,
                playbackSpeed = uiState.value.playbackSpeed,
                playbackPitchSemitone = uiState.value.playbackPitchSemitone,
                // 같은 곡이면 즐겨찾기 상태 유지, 곡이 바뀌면 observeFavorite의 flow가 새 값으로 갱신
                isFavorite = if (isSameMedia) uiState.value.isFavorite else false,
                // 새 곡의 가사는 저장소가 곧 알려 준다. 그때까지 이전 가사를 들고 있는다 — 아트워크가
                // 새 그림을 다 읽을 때까지 이전 그림을 들고 있는 것과 같다. 비워 두면 그 사이 가사 면이
                // 빈 채로 남고, 없다고 단정하면 아트워크로 샜다가 돌아와 앨범이 번쩍인다.
                hasLyrics = uiState.value.hasLyrics,
                lyricLines = uiState.value.lyricLines,
                lyricsId = uiState.value.lyricsId,
                lyricsMediaId = uiState.value.lyricsMediaId,
                activeLyricIndex = uiState.value.activeLyricIndex,
                isLyricsSynced = uiState.value.isLyricsSynced,
                isLyricsPanelVisible = uiState.value.isLyricsPanelVisible,
                isLyricsMissingNoticeVisible = isSameMedia && uiState.value.isLyricsMissingNoticeVisible,
                isLyricsFaceRequested = isSameMedia && uiState.value.isLyricsFaceRequested,
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
                    // 비밀 곡의 찜은 다른 플레이리스트로 가므로 둘 다 본다.
                    val isFavorite = aggregates?.any {
                        it.playlist.type == PlaylistType.FAVORITE || it.playlist.type == PlaylistType.SECRET_FAVORITE
                    } ?: false
                    val audioMedia = aggregates?.firstOrNull()?.audioMedia
                    _uiState.update {
                        it.copy(
                            isFavorite = isFavorite,
                            // 재생기 값은 곡을 걸 때 찍힌 스냅샷이라 저장소 값으로 덮는다.
                            title = audioMedia?.name ?: it.title,
                            artist = audioMedia?.artist ?: it.artist,
                        )
                    }
                    // hasLyrics 는 가사 내용과 함께 나가야 한다. 먼저 켜면 가사 면이 빈 채로 섰다가
                    // 줄이 도착할 때 한 번 더 겹쳐 지워진다.
                    loadLyricsIfNeeded(audioMedia)
                }
        }
    }

    private fun loadLyricsIfNeeded(audioMedia: AudioMedia?) {
        val mediaId = uiState.value.id
        val lyricFileFullPath = audioMedia?.lyricFileFullPath
        // 편집본이 생기거나 풀리면 같은 파일이라도 흐르는지가 달라져 다시 읽어야 한다.
        val isEdited = audioMedia?.editedAudioFileFullPath != null
        if (lyricFileFullPath == loadedLyricPath && isEdited == loadedLyricsEdited) {
            // 읽을 것은 없어도 이 답이 어느 곡의 것인지는 새로 적는다.
            _uiState.update { it.resolveLyrics(mediaId, hasLyrics = lyricFileFullPath != null) }
            return
        }
        loadedLyricPath = lyricFileFullPath
        loadedLyricsEdited = isEdited

        if (audioMedia == null || lyricFileFullPath == null) {
            parsedLyricLines = emptyList()
            _uiState.update {
                it.resolveLyrics(mediaId, hasLyrics = false).copy(
                    lyricLines = emptyList(),
                    lyricsId = null,
                    isLyricsSynced = false,
                    activeLyricIndex = -1,
                )
            }
            return
        }
        viewModelScope.launch {
            parsedLyricLines = getPlaybackLyricsUseCase(audioMedia)
            // 음차·번역 파일은 원본과 같은 타임코드로 만들어져 줄 번호가 맞는다
            val translationLines = getLyricsVariantUseCase(lyricFileFullPath, LyricsVariant.TRANSLATION).getOrNull()
            val transliterationLines = getLyricsVariantUseCase(lyricFileFullPath, LyricsVariant.TRANSLITERATION).getOrNull()
            _uiState.update {
                it.resolveLyrics(mediaId, hasLyrics = true).copy(
                    lyricLines = parsedLyricLines.mapIndexed { index, line ->
                        PlayerLyricLineUiState(
                            text = line.text,
                            transliteration = transliterationLines?.getOrNull(index)?.text?.takeIf(String::isNotBlank),
                            translation = translationLines?.getOrNull(index)?.text?.takeIf(String::isNotBlank),
                        )
                    },
                    lyricsId = lyricFileFullPath,
                    isLyricsSynced = parsedLyricLines.any { line -> line.timeMs != null },
                    activeLyricIndex = -1,
                )
            }
            refreshActiveLyricIndex()
        }
    }

    private fun refreshActiveLyricIndex() {
        nextLyricLineTimer.cancel()
        // 이전 곡 가사가 떠 있는 동안은 새 곡의 위치로 활성 줄을 다시 잡지 않는다 — 엉뚱한 줄이 뛴다.
        if (!uiState.value.isLyricsKnown) return
        val positionMs = mediaControllerManager.currentPosition ?: return
        applyActiveLyricIndex(positionMs)
    }

    private fun applyActiveLyricIndex(positionMs: Long) {
        val index = activeLyricIndex(parsedLyricLines, positionMs)
        if (index != _uiState.value.activeLyricIndex) {
            _uiState.update { it.copy(activeLyricIndex = index) }
        }
        if (!mediaControllerManager.isPlaying) return
        nextLyricLineTimer.schedule(parsedLyricLines, positionMs, mediaControllerManager.playbackSpeed) { reachedMs ->
            if (mediaControllerManager.isPlaying) applyActiveLyricIndex(reachedMs)
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
            val name = getPlaylistUseCase(playlistId).getOrNull()?.name.orEmpty()
            _uiState.update { it.copy(playlistName = name) }
        }
    }
}

