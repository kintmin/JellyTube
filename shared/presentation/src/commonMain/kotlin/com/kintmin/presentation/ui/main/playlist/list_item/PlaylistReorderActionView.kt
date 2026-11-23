package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
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
import com.kintmin.presentation.theme.contentDisabled
import com.kintmin.presentation.theme.contentSubtle

/** 색을 한 단계 낮춰 같은 머리말에 놓인 만들기가 주, 순서가 보조로 읽히게 한다. */
@Composable
fun PlaylistReorderActionView(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val reorderColor = if (enabled) contentSubtle else contentDisabled
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Rounded.SwapVert,
            contentDescription = null,
            tint = reorderColor,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "순서",
            style = MaterialTheme.typography.labelLarge,
            color = reorderColor,
        )
    }
}

@Preview
@Composable
fun PlaylistReorderActionViewPreview() {
    JellyTubeTheme {
        PlaylistReorderActionView(enabled = true, onClick = {})
    }
}
