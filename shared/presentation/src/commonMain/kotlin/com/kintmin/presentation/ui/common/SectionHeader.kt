package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted

/** 구분선과 여백으로만 구역을 가른다. [action] 은 머리말 끝에 붙는 그 구역 전용 동작이다. */
@Composable
fun SectionHeader(
    text: String,
    showDivider: Boolean = false,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        top = if (showDivider) 12.dp else 8.dp,
                        bottom = 8.dp,
                    )
                    .semantics { heading() },
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentMuted,
            )
            action?.invoke(this)
        }
    }
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    JellyTubeTheme {
        SectionHeader(text = "내 플레이리스트", showDivider = true)
    }
}
