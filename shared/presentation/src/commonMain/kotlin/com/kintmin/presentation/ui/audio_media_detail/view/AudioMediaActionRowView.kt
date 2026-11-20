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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.gray80

/**
 * 이 음원에 딸린 기능 한 줄(가사·노래방 번호·편집본). [onClick] 이 null 이면 눌리지 않는다.
 *
 * [description] 은 지금 값(노래방 번호, 편집본 길이)을, [trailing] 은 해제·되돌리기 버튼을 받는다.
 * [trailing] 을 주지 않고 눌릴 수 있는 줄에는 이동 화살표가 붙는다.
 */
@Composable
fun AudioMediaActionRowView(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) gray80 else gray40,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = if (enabled) MaterialTheme.colorScheme.onBackground else gray40,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = gray60,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = gray60,
            )
        }
    }
}

/** 연결을 끊는 줄의 trailing 버튼. 끊는 대상을 [contentDescription] 으로 말한다. */
@Composable
fun AudioMediaActionUnlinkButton(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = contentDescription,
            tint = if (enabled) gray60 else gray40,
        )
    }
}

/** 백그라운드 작업이 도는 줄의 trailing 표시. */
@Composable
fun AudioMediaActionProgressIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
    )
}

@Preview
@Composable
fun AudioMediaActionRowViewPreview() {
    JellyTubeTheme {
        AudioMediaActionRowView(
            icon = Icons.Rounded.Lyrics,
            title = "가사 찾기",
            onClick = {},
        )
    }
}

@Preview
@Composable
fun AudioMediaActionRowViewWithValuePreview() {
    JellyTubeTheme {
        AudioMediaActionRowView(
            icon = Icons.Rounded.Mic,
            title = "TJ 노래방 번호",
            description = "38087",
            onClick = {},
            trailing = {
                AudioMediaActionUnlinkButton(
                    contentDescription = "노래방 번호 연동 해제",
                    onClick = {},
                )
            },
        )
    }
}

@Preview
@Composable
fun AudioMediaActionRowViewDisabledPreview() {
    JellyTubeTheme {
        AudioMediaActionRowView(
            icon = Icons.Rounded.ContentCut,
            title = "편집본 저장 중",
            description = "저장이 끝나면 되돌릴 수 있습니다",
            onClick = {},
            enabled = false,
            trailing = { AudioMediaActionProgressIndicator() },
        )
    }
}
