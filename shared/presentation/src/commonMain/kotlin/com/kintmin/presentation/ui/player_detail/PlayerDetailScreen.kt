package com.kintmin.presentation.ui.player_detail

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.kintmin.presentation.animation.PLAYBACK_POLLING_INTERVAL_MILLIS
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.platform.extractDominantColor
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.deepSea100
import com.kintmin.presentation.theme.deepSea60
import com.kintmin.presentation.theme.deepSea80
import com.kintmin.presentation.theme.seaBlue40
import com.kintmin.presentation.theme.seaBlue80
import com.kintmin.presentation.ui.player_detail.control.PlaybackSliderView
import com.kintmin.presentation.ui.player_detail.control.PlaybackValueView
import com.kintmin.presentation.ui.player_detail.control.RepeatRangeMarkView
import com.kintmin.presentation.ui.player_detail.dialog.PlaybackPitchDialog
import com.kintmin.presentation.ui.player_detail.dialog.PlaybackSpeedDialog
import com.kintmin.presentation.ui.player_detail.dialog.SleepTimerAnnouncementDialog
import com.kintmin.presentation.ui.player_detail.dialog.SleepTimerDialog
import com.kintmin.presentation.ui.player_detail.dialog.toPitchSemitoneText
import com.kintmin.presentation.ui.player_detail.dialog.toPlaybackSpeedText
import com.kintmin.presentation.ui.player_detail.lyrics.PlayerLyricsPanelView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAudioMediaDetail: (audioMediaId: Int) -> Unit,
    navigateToAudioMediaInformationEdit: (audioMediaId: Int) -> Unit,
    navigateToPlayingPlaylist: (playlistId: Int, audioMediaId: Int?) -> Unit,
    navigateToLyricsSearch: (audioMediaId: Int, query: String, durationSeconds: Double?) -> Unit,
) {
    val viewModel = koinViewModel<PlayerDetailViewModel>()
    val data by viewModel.data.collectAsState()
    val toaster = rememberToaster()

    PlayerDetailScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = viewModel::sendIntent,
    )

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PlayerDetailEvent.NavigateToAudioMediaDetailScreen -> navigateToAudioMediaDetail(event.audioMediaId)
                is PlayerDetailEvent.NavigateToAudioMediaInformationEditScreen -> navigateToAudioMediaInformationEdit(event.audioMediaId)
                is PlayerDetailEvent.NavigateToLyricsSearch -> navigateToLyricsSearch(
                    event.audioMediaId,
                    event.query,
                    event.durationSeconds,
                )
                is PlayerDetailEvent.NavigateToPlayingPlaylist -> navigateToPlayingPlaylist(
                    event.playlistId,
                    event.audioMediaId
                )

                is PlayerDetailEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }
}

@Composable
fun PlayerDetailScreen(
    navigateToBack: () -> Unit,
    data: PlayerDetailUiState,
    sendIntent: (PlayerDetailIntent) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // RESUMED 동안만 폴링한다. repeatOnLifecycle 안에서 lifecycleScope.launch 로
    // 빠져나가면 화면을 벗어나도 루프가 취소되지 않아 백그라운드에서 계속 돈다.
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                delay(PLAYBACK_POLLING_INTERVAL_MILLIS.toLong())
                sendIntent(PlayerDetailIntent.OnRefreshMediaData)
            }
        }
        onPauseOrDispose { job.cancel() }
    }

    val playbackSeconds = max(1L, data.playbackDuration.inWholeSeconds)
    val currentSeconds = min(data.currentDuration.inWholeSeconds, playbackSeconds)
    var gradientColors by remember {
        mutableStateOf(
            listOf(
                Color(0xFF4B453B),
                Color(0xFF1D1C1A),
                Color(0xFF0D0D0D),
            ),
        )
    }
    LaunchedEffect(data.imageFileFullPath) {
        gradientColors = extractGradientFromArtwork(data.imageFileFullPath)
            ?: listOf(
                Color(0xFF4B453B),
                Color(0xFF1D1C1A),
                Color(0xFF0D0D0D),
            )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                    ),
                )
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 찜이 부가 줄로 내려가 좌우에 하나씩만 남았다. 그래서 제목이 가운데를 쓸 수 있다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로 가기",
                            tint = Color.White.copy(alpha = 0.82f),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(),
                            text = data.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(),
                            text = data.artist,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickMoreButton) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Subject,
                            contentDescription = "음원 정보",
                            tint = Color.White.copy(alpha = 0.82f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 가사 패널이 아트워크와 같은 자리·같은 크기를 쓴다. weight 를 바깥이 들고 있어야
                // 전환할 때 아래 요소들이 밀리지 않는다.
                Crossfade(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    targetState = data.showsLyricsPanel,
                ) { showsLyrics ->
                    // 가사 없는 곡에서는 clickable 을 붙이지 않는다 — 눌러도 할 일이 없으면
                    // 터치 피드백도 나지 않아야 한다.
                    val artworkModifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .let { base ->
                            if (data.hasLyrics) {
                                base.clickable { sendIntent(PlayerDetailIntent.OnClickLyrics) }
                            } else {
                                base
                            }
                        }

                    when {
                        showsLyrics -> PlayerLyricsPanelView(
                            lines = data.lyricLines,
                            activeIndex = data.activeLyricIndex,
                            modifier = artworkModifier,
                        )

                        data.imageFileFullPath == null -> Box(
                            modifier = artworkModifier,
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                modifier = Modifier.size(100.dp),
                                imageVector = Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }

                        else -> AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(data.imageFileFullPath.toPath())
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = artworkModifier,
                        )
                    }
                }

                // 아래 줄 사이를 넉넉히 벌린다. 아트워크가 남는 세로를 모두 가져가므로,
                // 여백을 늘리는 것과 아트워크를 줄이는 것이 같은 일이다.
                Spacer(modifier = Modifier.height(28.dp))

                // 세 상태가 모두 이 22dp 를 채운다. 비우면 가사 없는 곡에서 그만큼이 빈 자리로
                // 남고, 접으면 곡을 넘길 때 아트워크 크기가 흔들린다.
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clickable { sendIntent(PlayerDetailIntent.OnClickLyrics) }
                        .padding(horizontal = 16.dp),
                    text = when {
                        !data.hasLyrics -> "가사 찾기"
                        data.currentLyricLine.isNotBlank() -> data.currentLyricLine
                        else -> "가사 보기"
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = currentSeconds.seconds.to_hh_colon_mm_colon_ss(),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                    )
                    Text(
                        text = playbackSeconds.seconds.to_hh_colon_mm_colon_ss(),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                    )
                }

                PlaybackSliderView(
                    playbackSeconds = playbackSeconds,
                    currentPositionSeconds = (data.currentDuration.inWholeMilliseconds / 1000f)
                        .coerceAtMost(playbackSeconds.toFloat()),
                    isPlaying = data.isPlaying,
                    repeatRangeStartSeconds = data.repeatRangeStartDuration?.inWholeSeconds,
                    repeatRangeEndSeconds = data.repeatRangeEndDuration?.inWholeSeconds,
                    onValueChange = { sendIntent(PlayerDetailIntent.OnChangeTimeSlider(it)) },
                    onValueChangeFinished = { sendIntent(PlayerDetailIntent.OnChangeFinishTimeSlider) },
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 부가 기능 — 재생을 바꾸지 않고 이 곡·이 세션을 다루는 것들. 셋이라 넓게 벌어진다.
                // 가사는 아트워크와 한 줄 가사가 이미 입구라 여기 두지 않는다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickFavoriteButton) }) {
                        // 색으로 켜짐을 알리지 않는다. 밝은 파랑은 재생 모드가 켜졌다는 뜻으로
                        // 이미 쓰고 있어, 찜까지 같은 색이면 성격이 섞인다.
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = if (data.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (data.isFavorite) "찜 켜짐" else "찜 꺼짐",
                            tint = if (data.isFavorite) Color.White else Color.White.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickSleepTimerButton) }) {
                        // 켜짐을 tint 로만 알리지 않는다. 남은 시간은 뱃지가 말한다.
                        BadgedBox(
                            badge = {
                                data.sleepTimerRemainingMinutes?.let { minutes ->
                                    Badge(
                                        containerColor = seaBlue80,
                                        contentColor = deepSea100,
                                    ) {
                                        Text(text = "$minutes")
                                    }
                                }
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = data.sleepTimerRemainingMinutes
                                    ?.let { "잠들기 타이머 켜짐, ${it}분 남음" }
                                    ?: "잠들기 타이머",
                                tint = Color.White.copy(alpha = 0.82f),
                            )
                        }
                    }
                    RepeatRangeMarkView(
                        isStartSelected = data.repeatRangeStartDuration != null,
                        isEndSelected = data.repeatRangeEndDuration != null,
                        onClick = { sendIntent(PlayerDetailIntent.OnClickRepeatRangeButton) },
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 재생 기능 — 셔플과 반복이 양 끝을 잡아 좌 1 : 우 1 로 대칭이다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickShuffleButton) }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = if (data.isShuffling) "셔플 켜짐" else "셔플 꺼짐",
                            tint = if (data.isShuffling) seaBlue80 else Color.White.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickPreviousMediaButton) }) {
                        Icon(
                            modifier = Modifier.size(48.dp),
                            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                            contentDescription = "이전 음원",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        modifier = Modifier
                            .size(72.dp)
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.85f),
                                shape = CircleShape,
                            ),
                        onClick = { sendIntent(PlayerDetailIntent.OnClickPlayOrPauseButton) },
                    ) {
                        Icon(
                            modifier = Modifier.size(38.dp),
                            imageVector = if (data.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (data.isPlaying) "일시정지" else "재생",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickNextMediaButton) }) {
                        Icon(
                            modifier = Modifier.size(48.dp),
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = "다음 음원",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickRepeatButton) }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = if (data.isRepeating) "반복 켜짐" else "반복 꺼짐",
                            tint = if (data.isRepeating) seaBlue80 else Color.White.copy(alpha = 0.6f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 재생 부가 — 지금 걸려 있는 값과 어디서 재생 중인지.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaybackValueView(
                        imageVector = Icons.Rounded.MusicNote,
                        label = data.playbackPitchSemitone.toPitchSemitoneText(),
                        contentDescription = "피치",
                        onClick = { sendIntent(PlayerDetailIntent.OnClickPlaybackPitchButton) },
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { sendIntent(PlayerDetailIntent.OnClickPlayingPlaylistButton) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.82f),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (data.playlistName.isBlank()) "플레이리스트에서 재생중" else "${data.playlistName}에서 재생중",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PlaybackValueView(
                        imageVector = Icons.Rounded.Speed,
                        label = data.playbackSpeed.toPlaybackSpeedText(),
                        contentDescription = "재생 속도",
                        onClick = { sendIntent(PlayerDetailIntent.OnClickPlaybackSpeedButton) },
                    )
                }
            }

            PlaybackSpeedDialog(
                showDialog = data.isPlaybackSpeedMenuVisible,
                selectedSpeed = data.playbackSpeed,
                onDismiss = { sendIntent(PlayerDetailIntent.OnDismissPlaybackSpeedMenu) },
                onSelectSpeed = { sendIntent(PlayerDetailIntent.OnSelectPlaybackSpeed(it)) },
            )
            PlaybackPitchDialog(
                showDialog = data.isPlaybackPitchMenuVisible,
                selectedSemitone = data.playbackPitchSemitone,
                onDismiss = { sendIntent(PlayerDetailIntent.OnDismissPlaybackPitchMenu) },
                onSelectSemitone = { sendIntent(PlayerDetailIntent.OnSelectPlaybackPitchSemitone(it)) },
            )
            SleepTimerDialog(
                showDialog = data.isSleepTimerMenuVisible,
                minutes = data.sleepTimerMinutes,
                remaining = data.sleepTimerRemaining,
                announcementName = data.sleepTimerAnnouncement.selected?.name,
                onDismiss = { sendIntent(PlayerDetailIntent.OnDismissSleepTimerMenu) },
                onSelectMinutes = { sendIntent(PlayerDetailIntent.OnSelectSleepTimerMinutes(it)) },
                onClickAnnouncement = { sendIntent(PlayerDetailIntent.OnClickSleepTimerAnnouncementButton) },
                onStart = { sendIntent(PlayerDetailIntent.OnStartSleepTimer) },
                onCancelTimer = { sendIntent(PlayerDetailIntent.OnCancelSleepTimer) },
            )
            SleepTimerAnnouncementDialog(
                showDialog = data.isSleepTimerAnnouncementMenuVisible,
                selectedMediaId = data.sleepTimerAnnouncement.selected?.id,
                selectableMediaList = data.sleepTimerAnnouncement.selectable,
                onDismiss = { sendIntent(PlayerDetailIntent.OnDismissSleepTimerAnnouncementMenu) },
                onSelect = { sendIntent(PlayerDetailIntent.OnSelectSleepTimerAnnouncement(it)) },
            )
        }
    }
}

private suspend fun extractGradientFromArtwork(path: String?): List<Color>? {
    if (path.isNullOrBlank()) return null
    val seed = Color(extractDominantColor(path) ?: return null)

    return listOf(
        seed.lighten(0.2f),
        seed.darken(0.55f),
        seed.darken(0.82f),
    )
}


private fun Color.lighten(factor: Float): Color {
    val safe = factor.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * safe,
        green = green + (1f - green) * safe,
        blue = blue + (1f - blue) * safe,
        alpha = 1f,
    )
}

private fun Color.darken(factor: Float): Color {
    val safe = factor.coerceIn(0f, 1f)
    val mul = 1f - safe
    return Color(
        red = red * mul,
        green = green * mul,
        blue = blue * mul,
        alpha = 1f,
    )
}

@Preview
@Composable
private fun PlayerDetailScreenPreview() {
    JellyTubeTheme {
        PlayerDetailScreen(
            navigateToBack = {},
            data = PlayerDetailUiState.getMock(),
            sendIntent = {},
        )
    }
}



