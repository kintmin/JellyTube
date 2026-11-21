package com.kintmin.presentation.ui.audio_media_detail.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.accentContent
import com.kintmin.presentation.ui.audio_media_detail.AudioMediaDetailUiState

/**
 * 음원의 제목과 메타 정보. [onClickSource] 는 출처를 열 수 있을 때만 쓰인다.
 *
 * 출처는 웹 주소일 때만 누를 수 있다. 눌릴 수 없는 출처에 clickable 을 붙이면 눌러도 아무 일이
 * 일어나지 않는 자리가 생긴다.
 */
@Composable
fun AudioMediaDetailInfoView(
    data: AudioMediaDetailUiState,
    onClickSource: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = data.audioMediaName,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        if (data.artist.isNotBlank()) {
            Text(
                modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
                text = data.artist,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = contentMuted,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetaField(
                modifier = Modifier.weight(1f),
                label = "재생 시간",
                value = data.playTime,
            )
            // 생성 시각이 "2026년 11월 20일 14시 30분" 이라 길다. 재생 시간은 짧으니 폭을 몰아준다.
            MetaField(
                modifier = Modifier.weight(3f),
                label = "생성 시각",
                value = data.audioMediaCreationTime,
            )
        }

        if (data.webViewUrl != null) {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onClickSource(data.webViewUrl) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 아이콘을 라벨 앞에 두면 "출처" 가 다른 라벨보다 안쪽에서 시작해 축이 어긋난다.
                MetaField(
                    modifier = Modifier.weight(1f),
                    label = "출처",
                    value = data.webViewUrl,
                    valueColor = accentContent,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentContent,
                )
            }
        } else if (data.source.isNotBlank()) {
            MetaField(
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                label = "출처",
                value = data.source,
            )
        }

        if (data.audioMediaDescription.isNotBlank()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            // 설명은 길이가 정해지지 않아 MetaField 로 묶지 않는다. 그쪽은 값을 두 줄로 자른다.
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "설명",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = contentMuted,
            )
            // 여러 줄 단락이라 라벨과 줄 간격을 MetaField 의 한 줄 값보다 넉넉하게 둔다.
            Text(
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                text = data.audioMediaDescription,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
        }
    }
}

@Composable
private fun MetaField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = contentMuted,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
fun AudioMediaDetailInfoViewPreview() {
    JellyTubeTheme {
        AudioMediaDetailInfoView(
            data = AudioMediaDetailUiState.getMock(),
            onClickSource = {},
        )
    }
}

@Preview
@Composable
fun AudioMediaDetailInfoViewNotOpenableSourcePreview() {
    JellyTubeTheme {
        AudioMediaDetailInfoView(
            data = AudioMediaDetailUiState.getMock().copy(
                source = "fileShare://a1b2c3d4e5f6",
                webViewUrl = null,
            ),
            onClickSource = {},
        )
    }
}

@Preview
@Composable
fun AudioMediaDetailInfoViewLongSourcePreview() {
    JellyTubeTheme {
        AudioMediaDetailInfoView(
            data = AudioMediaDetailUiState.getMock().copy(
                webViewUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLabcdefghijklmnop&index=12",
            ),
            onClickSource = {},
        )
    }
}

@Preview
@Composable
fun AudioMediaDetailInfoViewNoDescriptionPreview() {
    JellyTubeTheme {
        AudioMediaDetailInfoView(
            data = AudioMediaDetailUiState.getMock().copy(audioMediaDescription = ""),
            onClickSource = {},
        )
    }
}
