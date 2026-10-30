package com.kintmin.presentation.ui.setting.section

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
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.gray80

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
    icon: ImageVector? = null,
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
        if (icon != null) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = icon,
                contentDescription = null,
                tint = gray80,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = gray60,
                )
            }
        }
        trailing?.invoke()
    }
}

/** 다른 화면으로 이동하는 항목의 trailing 화살표. */
@Composable
fun SettingNavigateIcon(contentDescription: String?) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = contentDescription,
        tint = gray60,
    )
}

@Preview(showBackground = true)
@Composable
fun SettingItemViewPreview() {
    JellyTubeTheme {
        SettingItemView(
            title = "버전",
            icon = Icons.Rounded.Info,
            description = "1.2.2",
            onClick = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingNavigateItemViewPreview() {
    JellyTubeTheme {
        SettingItemView(
            title = "PC에서 공유받기",
            icon = Icons.Rounded.Computer,
            onClick = {},
            trailing = { SettingNavigateIcon(contentDescription = null) },
        )
    }
}
