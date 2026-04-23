package com.kintmin.presentation.ui.player_detail

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

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
                is PlayerDetailEvent.NavigateToPlayingPlaylist -> navigateToPlayingPlaylist(event.playlistId, event.audioMediaId)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                ),
            ),
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
                        imageVector = Icons.Rounded.KeyboardArrowDown,
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
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "더보기",
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
                IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickAddButton) }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "추가",
                        tint = Color.White.copy(alpha = 0.82f),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = data.title,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = data.artist,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 18.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }

                IconButton(onClick = { sendIntent(PlayerDetailIntent.OnClickMoreButton) }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "옵션",
                        tint = Color.White.copy(alpha = 0.82f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = toMinSec(currentSeconds),
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                )
                Text(
                    text = toMinSec(playbackSeconds),
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
                        tint = if (data.isShuffling) Color(0xFFF3CC53) else Color.White.copy(alpha = 0.86f),
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
                        .size(96.dp)
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
                        tint = if (data.isRepeating) Color(0xFFF3CC53) else Color.White.copy(alpha = 0.86f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable { sendIntent(PlayerDetailIntent.OnClickPlayingPlaylistButton) },
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
                )
            }
        }
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
        inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 240, 240)
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

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2
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

private fun toMinSec(seconds: Long): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "$min:${sec.toString().padStart(2, '0')}"
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
