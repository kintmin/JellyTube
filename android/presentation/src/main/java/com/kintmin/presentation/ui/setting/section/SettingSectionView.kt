package com.kintmin.presentation.ui.setting.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60

/**
 * 설정 화면의 한 섹션. 섹션 제목 헤더 아래에 항목들을 세로로 배치한다.
 * [showDivider]가 true면 헤더 위에 흐린 구분선을 그린다(첫 섹션은 false).
 */
@Composable
fun SettingSectionView(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Text(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = gray60,
        )
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingSectionViewPreview() {
    JellyTubeTheme {
        SettingSectionView(title = "다운로드") {
            SettingItemView(
                title = "다운로드 시 재생목록",
                description = "기본",
                onClick = {},
            )
        }
    }
}
