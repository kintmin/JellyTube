package com.kintmin.presentation.ui.setting.section

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme

/**
 * 설정 화면의 한 항목. [onClick]이 null이면 정보 표시용으로 클릭되지 않는다.
 * [description]은 제목 아래 보조 문구(현재 값 또는 설명)로 표시하고,
 * [trailing]에는 Switch·진행 표시·이동 화살표 등을 넣는다.
 */
@Composable
fun SettingItemView(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) {
            modifier.clickable(enabled = enabled, onClick = onClick)
        } else {
            modifier
        },
        headlineContent = { Text(text = title) },
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = trailing,
    )
}

/** 다른 화면으로 이동하는 항목의 trailing 화살표. */
@Composable
fun SettingNavigateIcon(contentDescription: String?) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = contentDescription,
    )
}

@Preview(showBackground = true)
@Composable
fun SettingItemViewPreview() {
    JellyTubeTheme {
        SettingItemView(
            title = "이상 데이터 점검 및 제거",
            description = "음원 추가 오류 시 권장",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingNavigateItemViewPreview() {
    JellyTubeTheme {
        SettingItemView(
            title = "PC에서 공유받기",
            onClick = {},
            trailing = { SettingNavigateIcon(contentDescription = null) },
        )
    }
}
