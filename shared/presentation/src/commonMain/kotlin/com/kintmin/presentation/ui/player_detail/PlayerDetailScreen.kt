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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.animation.PlaybackPollingEffect
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.platform.extractDominantColor
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkBackdropFallback
import com.kintmin.presentation.theme.overlayContent
import com.kintmin.presentation.theme.overlayContentMuted
import com.kintmin.presentation.theme.overlayContentSecondary
import com.kintmin.presentation.theme.overlayScrimSoft
import com.kintmin.presentation.ui.common.AudioArtwork
import com.kintmin.presentation.ui.common.PlaybackToggleButton
import com.kintmin.presentation.ui.player_detail.control.PLAYBACK_VALUE_WIDTH
import com.kintmin.presentation.ui.player_detail.control.PlaybackSliderView
import com.kintmin.presentation.ui.player_detail.control.PlaybackValueView
import com.kintmin.presentation.ui.player_detail.control.RepeatRangeMarkView
import com.kintmin.presentation.ui.player_detail.dialog.PlaybackPitchDialog
import com.kintmin.presentation.ui.player_detail.dialog.PlaybackSpeedDialog
import com.kintmin.presentation.ui.player_detail.dialog.SleepTimerAnnouncementDialog
import com.kintmin.presentation.ui.player_detail.dialog.SleepTimerDialog
import com.kintmin.presentation.ui.player_detail.dialog.toPitchSemitoneText
import com.kintmin.presentation.ui.player_detail.dialog.toPlaybackSpeedText
import com.kintmin.presentation.ui.player_detail.lyrics.CurrentLyricOverlayView
import com.kintmin.presentation.ui.player_detail.lyrics.PlayerLyricsPanelView
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayerDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAudioMediaDetail: (audioMediaId: Int) -> Unit,
    navigateToAudioMediaInformationEdit: (audioMediaId: Int) -> Unit,
    navigateToPlayingPlaylist: (playlistId: Int, audioMediaId: Int?) -> Unit,
    navigateToLyricsSearch: (audioMediaId: Int, query: String, durationSeconds: Double?) -> Unit,
    navigateToLyricsViewer: (audioMediaId: Int) -> Unit,
) {
    val viewModel = koinViewModel<PlayerDetailViewModel>()
    val data by viewModel.uiState.collectAsState()
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
                is PlayerDetailEvent.NavigateToLyricsViewer -> navigateToLyricsViewer(event.audioMediaId)
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
    PlaybackPollingEffect { sendIntent(PlayerDetailIntent.OnRefreshMediaData) }

    val playbackSeconds = max(1L, data.playbackDuration.inWholeSeconds)
    val currentSeconds = min(data.currentDuration.inWholeSeconds, playbackSeconds)
    var gradientColors by remember { mutableStateOf(artworkBackdropFallback) }
    LaunchedEffect(data.imageFileFullPath) {
        gradientColors = extractGradientFromArtwork(data.imageFileFullPath) ?: artworkBackdropFallback
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                            tint = overlayContentSecondary,
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
                            color = overlayContent,
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
                            color = overlayContentMuted,
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
                            tint = overlayContentSecondary,
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
                    // 가사 없는 곡이면 찾으러 보낸다 — 그 안내를 화면에 상시로 띄우지 않고 눌렀을 때만 꺼낸다.
                    // 패널에서는 싱크 가사의 줄이 탭을 먼저 받고, 줄 밖을 누르면 아트워크로 돌아간다.
                    val artworkModifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(overlayScrimSoft)
                        .clickable { sendIntent(PlayerDetailIntent.OnClickLyrics) }

                    if (showsLyrics) {
                        Box(modifier = artworkModifier) {
                            PlayerLyricsPanelView(
                                lines = data.lyricLines,
                                activeIndex = data.activeLyricIndex,
                                onClickLine = if (data.isLyricsSynced) {
                                    { index -> sendIntent(PlayerDetailIntent.OnClickLyricLine(index)) }
                                } else {
                                    null
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = { sendIntent(PlayerDetailIntent.OnClickLyricsViewerButton) },
                            ) {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = Icons.Rounded.OpenInFull,
                                    contentDescription = "가사 크게 보기",
                                    tint = overlayContentSecondary,
                                )
                            }
                        }
                    } else {
                        AudioArtwork(
                            imageFileFullPath = data.imageFileFullPath,
                            modifier = artworkModifier,
                            placeholderIconSize = 100.dp,
                            placeholderTint = overlayContentMuted,
                        ) {
                            CurrentLyricOverlayView(
                                line = data.currentLyricLine,
                                translation = data.currentLyricTranslation,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 칸마다 폭을 똑같이 나눠 준다. SpaceEvenly 는 간격만 같게 해서, 글자인 A-B 가
                // 아이콘보다 넓으면 가운데 것이 화면 중앙에서 밀린다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickFavoriteButton) }) {
                            // 색으로 켜짐을 알리지 않는다. 파랑은 재생 모드가 켜졌다는 뜻으로
                            // 이미 쓰고 있어, 찜까지 같은 색이면 성격이 섞인다.
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = if (data.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (data.isFavorite) "찜 켜짐" else "찜 꺼짐",
                                tint = if (data.isFavorite) overlayContent else overlayContentMuted,
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        // 켜짐을 tint 로만 알리지 않는다. 남은 시간은 뱃지가 말한다.
                        // IconButton 은 자기 영역을 원으로 잘라, 안에 두면 모서리로 나간 뱃지가 잘린다.
                        BadgedBox(
                            badge = {
                                data.sleepTimerRemainingMinutes?.let { minutes ->
                                    Badge(
                                        // 48dp 버튼이 아니라 가운데 24dp 아이콘의 모서리에 붙인다.
                                        modifier = Modifier.offset(x = (-12).dp, y = 12.dp),
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary,
                                    ) {
                                        Text(text = "$minutes")
                                    }
                                }
                            },
                        ) {
                            IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickSleepTimerButton) }) {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = Icons.Rounded.Bedtime,
                                    contentDescription = data.sleepTimerRemainingMinutes
                                        ?.let { "잠들기 타이머 켜짐, ${it}분 남음" }
                                        ?: "잠들기 타이머",
                                    tint = overlayContentSecondary,
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickLyrics) }) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Rounded.Lyrics,
                                contentDescription = if (data.showsLyricsPanel) "가사 닫기" else "가사 보기",
                                tint = if (data.showsLyricsPanel) overlayContent else overlayContentMuted,
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        RepeatRangeMarkView(
                            isStartSelected = data.repeatRangeStartDuration != null,
                            isEndSelected = data.repeatRangeEndDuration != null,
                            onClick = { sendIntent(PlayerDetailIntent.OnClickRepeatRangeButton) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = currentSeconds.seconds.to_hh_colon_mm_colon_ss(),
                        color = overlayContentSecondary,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = playbackSeconds.seconds.to_hh_colon_mm_colon_ss(),
                        color = overlayContentSecondary,
                        fontSize = 13.sp,
                    )
                }

                // 재생바는 재생 버튼 바로 위에 둔다 — 끌어서 맞추고 바로 누르는 자리다.
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

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PlaybackToggleButton(
                            imageVector = Icons.Rounded.Shuffle,
                            label = "셔플",
                            isOn = data.isShuffling,
                            iconSize = 24.dp,
                            offColor = overlayContentMuted,
                            onClick = { sendIntent(PlayerDetailIntent.OnClickShuffleButton) },
                        )
                    }
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickPreviousMediaButton) }) {
                        Icon(
                            modifier = Modifier.size(48.dp),
                            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                            contentDescription = "이전 음원",
                            tint = overlayContent,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        modifier = Modifier
                            .size(72.dp)
                            .border(
                                width = 2.dp,
                                color = overlayContentSecondary,
                                shape = CircleShape,
                            ),
                        onClick = { sendIntent(PlayerDetailIntent.OnClickPlayOrPauseButton) },
                    ) {
                        Icon(
                            modifier = Modifier.size(38.dp),
                            imageVector = if (data.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (data.isPlaying) "일시정지" else "재생",
                            tint = overlayContent,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickNextMediaButton) }) {
                        Icon(
                            modifier = Modifier.size(48.dp),
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = "다음 음원",
                            tint = overlayContent,
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PlaybackToggleButton(
                            imageVector = Icons.Rounded.Repeat,
                            label = "반복",
                            isOn = data.isRepeating,
                            iconSize = 24.dp,
                            offColor = overlayContentMuted,
                            onClick = { sendIntent(PlayerDetailIntent.OnClickRepeatButton) },
                        )
                    }
                }

                // 48dp 다. 이 간격이 재생 버튼의 높이를 정한다 — 아래 두 줄의 두께가 고정이라
                // 여기를 줄이면 버튼이 그만큼 내려앉는다. 손이 기억하는 자리라 바꾸지 않는다.
                Spacer(modifier = Modifier.height(48.dp))

                // 양 끝을 같은 폭으로 잡아야 가운데 글자가 화면 중앙에 선다.
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
                        modifier = Modifier.width(PLAYBACK_VALUE_WIDTH),
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { sendIntent(PlayerDetailIntent.OnClickPlayingPlaylistButton) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = overlayContentSecondary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (data.playlistName.isBlank()) "플레이리스트에서 재생중" else "${data.playlistName}에서 재생중",
                            color = overlayContentSecondary,
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
                        modifier = Modifier.width(PLAYBACK_VALUE_WIDTH),
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



