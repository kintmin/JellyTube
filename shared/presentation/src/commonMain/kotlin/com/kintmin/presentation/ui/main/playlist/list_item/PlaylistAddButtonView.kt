package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.seaBlue60

/**
 * 섹션 머리말에 붙는 만들기 입구.
 *
 * 격자 맨 끝 타일이 아니라 여기 둔다. 끝에 두면 플레이리스트가 늘수록 자리가 밀려 화면 밖으로
 * 내려가는데, 만드는 일은 보는 일보다 드물어도 자리는 고정돼 있어야 찾을 수 있다.
 */
@Composable
fun PlaylistAddButtonView(onClick: () -> Unit) {
    // ink 를 쓰지 않는다. 이 앱에 강조색 글자 버튼에 ink 를 두는 자리가 없어 혼자 튄다.
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = seaBlue60,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "새로 만들기",
            style = MaterialTheme.typography.labelLarge,
            color = seaBlue60,
        )
    }
}

@Preview
@Composable
fun PlaylistAddButtonPreview() {
    JellyTubeTheme {
        PlaylistAddButtonView(onClick = {})
    }
}
