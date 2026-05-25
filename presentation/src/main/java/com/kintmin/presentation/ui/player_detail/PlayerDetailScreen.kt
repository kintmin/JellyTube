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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.LibraryMusic
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.kintmin.presentation.theme.deepSea60
import com.kintmin.presentation.theme.deepSea80
import com.kintmin.presentation.theme.seaBlue10
import com.kintmin.presentation.theme.seaBlue40
import com.kintmin.presentation.ui.player_detail.dialog.PlaybackPitchDialog
import com.kintmin.presentation.ui.player_detail.dialog.PlaybackSpeedDialog
import com.kintmin.presentation.ui.player_detail.dialog.toPitchSemitoneText
import com.kintmin.presentation.ui.player_detail.dialog.toPlaybackSpeedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
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
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = navigateToBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로 가기",
                            tint = Color.White.copy(alpha = 0.82f),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM ARTIST",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(),
                            text = data.artist,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = { sendIntent(PlayerDetailIntent.OnClickMoreButton) }) {
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
                    modifier = Modifier.fillMaxWidth().height(78.dp),
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

                PlaybackSlider(
                    playbackSeconds = playbackSeconds,
                    currentSeconds = currentSeconds,
                    repeatRangeStartSeconds = data.repeatRangeStartDuration?.inWholeSeconds,
                    repeatRangeEndSeconds = data.repeatRangeEndDuration?.inWholeSeconds,
                    onValueChange = { sendIntent(PlayerDetailIntent.OnChangeTimeSlider(it)) },
                    onValueChangeFinished = { sendIntent(PlayerDetailIntent.OnChangeFinishTimeSlider) },
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickShuffleButton) }) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "셔플",
                                tint = if (data.isShuffling) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.86f),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickPreviousMediaButton) }) {
                            Icon(
                                modifier = Modifier.size(40.dp),
                                imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                                contentDescription = "이전 음원",
                                tint = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                                contentDescription = "재생/일시정지",
                                tint = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickNextMediaButton) }) {
                            Icon(
                                modifier = Modifier.size(40.dp),
                                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                contentDescription = "다음 음원",
                                tint = Color.White,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        RepeatRangeButton(
                            isStartSelected = data.repeatRangeStartDuration != null,
                            isEndSelected = data.repeatRangeEndDuration != null,
                            onClick = { sendIntent(PlayerDetailIntent.OnClickRepeatRangeButton) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(96.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackPitchChip(
                    Modifier.width(52.dp),
                    semitone = data.playbackPitchSemitone,
                    onClick = { sendIntent(PlayerDetailIntent.OnClickPlaybackPitchButton) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { sendIntent(PlayerDetailIntent.OnClickPlayingPlaylistButton) }
                            .padding(vertical  = 6.dp),
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
                }
                PlaybackSpeedChip(
                    Modifier.width(64.dp),
                    speed = data.playbackSpeed,
                    onClick = { sendIntent(PlayerDetailIntent.OnClickPlaybackSpeedButton) },
                )
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
        }
    }
}

@Composable
private fun PlaybackSlider(
    playbackSeconds: Long,
    currentSeconds: Long,
    repeatRangeStartSeconds: Long?,
    repeatRangeEndSeconds: Long?,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    var sliderWidth by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .onSizeChanged { sliderWidth = it.width },
    ) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..playbackSeconds.toFloat(),
            value = currentSeconds.toFloat(),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
            ),
        )
        RepeatRangeMarker(
            label = "A",
            seconds = repeatRangeStartSeconds,
            playbackSeconds = playbackSeconds,
            sliderWidth = sliderWidth,
        )
        RepeatRangeMarker(
            label = "B",
            seconds = repeatRangeEndSeconds,
            playbackSeconds = playbackSeconds,
            sliderWidth = sliderWidth,
        )
    }
}

@Composable
private fun RepeatRangeMarker(
    label: String,
    seconds: Long?,
    playbackSeconds: Long,
    sliderWidth: Int,
) {
    if (seconds == null || sliderWidth == 0) return

    val density = LocalDensity.current
    val markerSize = 18.dp
    val markerWidth = 42.dp
    val barWidth = 2.dp
    val markerOffset = with(density) {
        (sliderWidth * (seconds.toFloat() / playbackSeconds).coerceIn(0f, 1f)).toDp()
    }
    Column(
        modifier = Modifier
            .offset(x = markerOffset - markerWidth / 2)
            .width(markerWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(markerSize)
                .clip(CircleShape)
                .background(seaBlue10),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(22.dp)
                .background(seaBlue10),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = seconds.seconds.to_hh_colon_mm_colon_ss(),
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 10.sp,
            lineHeight = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun RepeatRangeButton(
    modifier: Modifier = Modifier,
    isStartSelected: Boolean,
    isEndSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "A",
            color = if (isStartSelected) seaBlue10 else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = " | ",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "B",
            color = if (isEndSelected) seaBlue10 else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PlaybackPitchChip(
    modifier: Modifier = Modifier,
    semitone: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
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
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = speed.toPlaybackSpeedText(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
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

