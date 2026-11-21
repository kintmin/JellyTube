package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentDisabled
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.contentSubtle

/** 목록이 비었을 때. [actionText] 를 주면 비운 상태를 벗어날 주 동작 버튼을 단다. */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    onClickAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = icon,
            contentDescription = null,
            tint = contentDisabled,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentSubtle,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (actionText != null) {
            Button(
                modifier = Modifier.padding(top = 24.dp),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                onClick = onClickAction,
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = actionText)
            }
        }
    }
}

@Preview
@Composable
private fun EmptyStateViewPreview() {
    JellyTubeTheme {
        EmptyStateView(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.LibraryMusic,
            title = "담긴 음원이 없습니다",
            description = "곡을 담아 두면 한 번에 재생할 수 있습니다",
            actionText = "곡 추가하기",
        )
    }
}
