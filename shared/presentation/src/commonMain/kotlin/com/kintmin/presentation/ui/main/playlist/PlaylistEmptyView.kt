package com.kintmin.presentation.ui.main.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.gray80

// 격자 항목은 뷰포트 높이를 알 수 없어 남은 공간을 채울 수 없다. 머리말과 하단 바를 뺀
// 자리의 가운데쯤에 오도록 잡은 값이다.
private val EMPTY_HEIGHT = 280.dp

@Composable
fun PlaylistEmptyView(
    modifier: Modifier,
    onClickAdd: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EMPTY_HEIGHT)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Rounded.QueueMusic,
            contentDescription = null,
            tint = gray40,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "만든 플레이리스트가 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = gray80,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "듣고 싶은 곡을 모아 두면 한 번에 재생할 수 있습니다",
            style = MaterialTheme.typography.bodySmall,
            color = gray60,
            textAlign = TextAlign.Center,
        )
        Button(
            modifier = Modifier.padding(top = 24.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
            onClick = onClickAdd,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = "새 플레이리스트 만들기",
                fontSize = 14.sp,
                lineHeight = 14.sp,
            )
        }
    }
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
