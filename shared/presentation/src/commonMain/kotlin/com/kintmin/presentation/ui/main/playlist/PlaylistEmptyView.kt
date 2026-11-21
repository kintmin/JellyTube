package com.kintmin.presentation.ui.main.playlist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.EmptyStateView

// 격자 항목은 뷰포트 높이를 알 수 없어 남은 공간을 채울 수 없다. 머리말과 하단 바를 뺀
// 자리의 가운데쯤에 오도록 잡은 값이다.
private val EMPTY_HEIGHT = 280.dp

@Composable
fun PlaylistEmptyView(
    modifier: Modifier,
    onClickAdd: () -> Unit,
) {
    EmptyStateView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EMPTY_HEIGHT),
        icon = Icons.Rounded.QueueMusic,
        title = "만든 플레이리스트가 없습니다",
        description = "듣고 싶은 곡을 모아 두면 한 번에 재생할 수 있습니다",
        actionText = "새 플레이리스트 만들기",
        onClickAction = onClickAdd,
    )
}

@Preview
@Composable
fun PlaylistEmptyPreview() {
    JellyTubeTheme {
        PlaylistEmptyView(
            modifier = Modifier,
            onClickAdd = {},
        )
    }
}
