package com.kintmin.presentation.ui.player_detail

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAudioMediaDetail: (audioMediaId: Int) -> Unit,
    navigateToAudioMediaEdit: (audioMediaId: Int) -> Unit,
    navigateToPlayingPlaylist: (playlistId: Int, audioMediaId: Int?) -> Unit,
) {
    val viewModel = hiltViewModel<PlayerDetailViewModel>()
    val data by viewModel.data.collectAsState()
    val context = LocalContext.current

    PlayerDetailScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = viewModel::sendIntent,
    )

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PlayerDetailEvent.NavigateToAudioMediaDetailScreen -> navigateToAudioMediaDetail(event.audioMediaId)
                is PlayerDetailEvent.NavigateToAudioMediaEditScreen -> navigateToAudioMediaEdit(event.audioMediaId)
                is PlayerDetailEvent.NavigateToPlayingPlaylist -> navigateToPlayingPlaylist(
                    event.playlistId,
                    event.audioMediaId
                )

                is PlayerDetailEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
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
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            lifecycleOwner.lifecycleScope.launch {
                while (isActive) {
                    delay(300)
                    sendIntent(PlayerDetailIntent.OnRefreshMediaData)
                }
            }
        }
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로 가기",
                            tint = Color.White.copy(alpha = 0.82f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM ARTIST",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = data.artist,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickMoreButton) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Subject,
                            contentDescription = "세부 화면",
                            tint = Color.White.copy(alpha = 0.82f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (data.imageFileFullPath == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            modifier = Modifier.size(100.dp),
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(data.imageFileFullPath))
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Black.copy(alpha = 0.25f)),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(),
                            text = data.title,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .basicMarquee(),
                            text = data.artist,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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

                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 0f..playbackSeconds.toFloat(),
                    value = currentSeconds.toFloat(),
                    onValueChange = { sendIntent(PlayerDetailIntent.OnChangeTimeSlider(it)) },
                    onValueChangeFinished = { sendIntent(PlayerDetailIntent.OnChangeFinishTimeSlider) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                    ),
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickShuffleButton) }) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "셔플",
                            tint = if (data.isShuffling) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.86f),
                        )
                    }
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickPreviousMediaButton) }) {
                        Icon(
                            modifier = Modifier.size(40.dp),
                            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                            contentDescription = "이전 음원",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .size(84.dp)
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.85f),
                                shape = CircleShape,
                            ),
                        onClick = { sendIntent(PlayerDetailIntent.OnClickPlayOrPauseButton) },
                    ) {
                        Icon(
                            modifier = Modifier.size(44.dp),
                            imageVector = if (data.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "재생/일시정지",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickNextMediaButton) }) {
                        Icon(
                            modifier = Modifier.size(40.dp),
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = "다음 음원",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickRepeatButton) }) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "반복",
                            tint = if (data.isRepeating) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.86f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlaybackPitchChip(
                        modifier = Modifier.align(Alignment.CenterStart),
                        semitone = data.playbackPitchSemitone,
                        onClick = { sendIntent(PlayerDetailIntent.OnClickPlaybackPitchButton) },
                    )
                    Row(
                        modifier = Modifier
                            .clickable { sendIntent(PlayerDetailIntent.OnClickPlayingPlaylistButton) }
                            .padding(horizontal = 48.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.82f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (data.playlistName.isBlank()) "플레이리스트에서 재생중" else "${data.playlistName}에서 재생중",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PlaybackSpeedChip(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        speed = data.playbackSpeed,
                        onClick = { sendIntent(PlayerDetailIntent.OnClickPlaybackSpeedButton) },
                    )
                }
            }

            if (data.isPlaybackSpeedMenuVisible) {
                PlaybackSpeedMenu(
                    selectedSpeed = data.playbackSpeed,
                    onDismiss = { sendIntent(PlayerDetailIntent.OnDismissPlaybackSpeedMenu) },
                    onSelectSpeed = { sendIntent(PlayerDetailIntent.OnSelectPlaybackSpeed(it)) },
                )
            }
            if (data.isPlaybackPitchMenuVisible) {
                PlaybackPitchMenu(
                    selectedSemitone = data.playbackPitchSemitone,
                    onDismiss = { sendIntent(PlayerDetailIntent.OnDismissPlaybackPitchMenu) },
                    onSelectSemitone = { sendIntent(PlayerDetailIntent.OnSelectPlaybackPitchSemitone(it)) },
                )
            }
        }
    }
}

@Composable
private fun PlaybackPitchChip(
    modifier: Modifier = Modifier,
    semitone: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = semitone.toPitchSemitoneText(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PlaybackSpeedChip(
    modifier: Modifier = Modifier,
    speed: Float,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = speed.toPlaybackSpeedText(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PlaybackPitchMenu(
    selectedSemitone: Int,
    onDismiss: () -> Unit,
    onSelectSemitone: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.42f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "피치",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = selectedSemitone.toPitchSemitoneText(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaybackSpeedStepButton(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "피치 낮추기",
                        onClick = {
                            selectedSemitone.previousPitchSemitone()?.let(onSelectSemitone)
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = selectedSemitone.toFloat(),
                        onValueChange = { value ->
                            val semitone =
                                value.roundToInt().coerceIn(MinPlaybackPitchSemitone, MaxPlaybackPitchSemitone)
                            if (semitone != selectedSemitone) {
                                onSelectSemitone(semitone)
                            }
                        },
                        valueRange = MinPlaybackPitchSemitone.toFloat()..MaxPlaybackPitchSemitone.toFloat(),
                        steps = MaxPlaybackPitchSemitone - MinPlaybackPitchSemitone - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.36f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PlaybackSpeedStepButton(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "피치 높이기",
                        onClick = {
                            selectedSemitone.nextPitchSemitone()?.let(onSelectSemitone)
                        },
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PlaybackPitchOptions.forEach { semitone ->
                        PlaybackPitchOptionChip(
                            semitone = semitone,
                            isSelected = selectedSemitone == semitone,
                            onClick = { onSelectSemitone(semitone) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackSpeedMenu(
    selectedSpeed: Float,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.42f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "재생 속도",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = selectedSpeed.toPlaybackSpeedText(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaybackSpeedStepButton(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "재생 속도 낮추기",
                        onClick = {
                            selectedSpeed.previousPlaybackSpeed()?.let(onSelectSpeed)
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = selectedSpeed.coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed),
                        onValueChange = { value ->
                            val speed = value.roundToPlaybackSpeedStep()
                            if (speed != selectedSpeed) {
                                onSelectSpeed(speed)
                            }
                        },
                        valueRange = MinPlaybackSpeed..MaxPlaybackSpeed,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.36f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PlaybackSpeedStepButton(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "재생 속도 높이기",
                        onClick = {
                            selectedSpeed.nextPlaybackSpeed()?.let(onSelectSpeed)
                        },
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PlaybackSpeedOptions.forEach { speed ->
                        PlaybackSpeedOptionChip(
                            speed = speed,
                            isSelected = selectedSpeed == speed,
                            onClick = { onSelectSpeed(speed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackSpeedStepButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PlaybackPitchOptionChip(
    semitone: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (isSelected) 0.26f else 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = semitone.toPitchSemitoneText(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PlaybackSpeedOptionChip(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (isSelected) 0.26f else 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = speed.formatPlaybackSpeed(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val MinPlaybackPitchSemitone = -10
private const val MaxPlaybackPitchSemitone = 10
private const val PlaybackPitchStep = 1

private val PlaybackPitchOptions = listOf(-4, -2, 0, 2, 4)

private const val MinPlaybackSpeed = 0.25f
private const val MaxPlaybackSpeed = 3.0f
private const val PlaybackSpeedStep = 0.01f

private val PlaybackSpeedOptions = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

private fun Int.toPitchSemitoneText(): String {
    return when {
        this > 0 -> "+$this"
        else -> toString()
    }
}

private fun Int.previousPitchSemitone(): Int? {
    return (this - PlaybackPitchStep).takeIf { it >= MinPlaybackPitchSemitone }
}

private fun Int.nextPitchSemitone(): Int? {
    return (this + PlaybackPitchStep).takeIf { it <= MaxPlaybackPitchSemitone }
}

private fun Float.toPlaybackSpeedText(): String = "x${formatPlaybackSpeed()}"

private fun Float.previousPlaybackSpeed(): Float? {
    return (this - PlaybackSpeedStep)
        .takeIf { it >= MinPlaybackSpeed }
        ?.roundToPlaybackSpeedStep()
}

private fun Float.nextPlaybackSpeed(): Float? {
    return (this + PlaybackSpeedStep)
        .takeIf { it <= MaxPlaybackSpeed }
        ?.roundToPlaybackSpeedStep()
}

private fun Float.roundToPlaybackSpeedStep(): Float {
    return (this / PlaybackSpeedStep).roundToInt()
        .times(PlaybackSpeedStep)
        .coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed)
}

private fun Float.formatPlaybackSpeed(): String {
    return if (this % 1.0f == 0f) {
        String.format(Locale.ROOT, "%.1f", this)
    } else {
        String.format(Locale.ROOT, "%.2f", this).trimEnd('0')
    }
}

private suspend fun extractGradientFromArtwork(path: String?): List<Color>? = withContext(Dispatchers.IO) {
    if (path.isNullOrBlank()) return@withContext null

    val file = File(path)
    if (!file.exists()) return@withContext null

    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

    val sampledOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight)
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    val bitmap = BitmapFactory.decodeFile(path, sampledOptions) ?: return@withContext null

    runCatching {
        val palette = Palette.Builder(bitmap)
            .maximumColorCount(16)
            .generate()
        val seed = palette.getDominantColor(0xFF4B453B.toInt())

        val top = Color(seed).lighten(0.2f)
        val middle = Color(seed).darken(0.55f)
        val bottom = Color(seed).darken(0.82f)

        listOf(top, middle, bottom)
    }.getOrNull().also {
        bitmap.recycle()
    }
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int = 240, reqHeight: Int = 240): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
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

@Preview(showBackground = true)
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

