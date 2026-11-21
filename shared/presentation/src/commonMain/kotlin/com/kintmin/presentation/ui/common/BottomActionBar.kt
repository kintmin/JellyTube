package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentDisabled
import com.kintmin.presentation.theme.floatingElevation

/** 목록을 이 자리 밑까지 그려야 한다 — innerPadding 은 목록의 contentPadding 으로 준다. */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(FloatingExternalPadding),
        content = content,
    )
}

@Composable
fun BottomActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        // 글자 길이에 맞추면 선택 수가 바뀔 때마다 너비가 흔들린다.
        modifier = modifier
            .fillMaxWidth()
            .height(BottomActionButtonHeight),
        shape = CircleShape,
        enabled = enabled,
        // 기본 비활성 바탕은 반투명이라 목록 위에 뜨면 아래 행이 비친다.
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = contentDisabled,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = floatingElevation,
            pressedElevation = floatingElevation,
            disabledElevation = floatingElevation,
        ),
        onClick = onClick,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val FloatingExternalPadding = 16.dp
private val BottomActionButtonHeight = 48.dp

@Preview
@Composable
fun BottomActionBarPreview() {
    JellyTubeTheme {
        BottomActionBar {
            BottomActionButton(
                text = "3곡 플레이리스트에서 빼기",
                onClick = {},
            )
        }
    }
}
