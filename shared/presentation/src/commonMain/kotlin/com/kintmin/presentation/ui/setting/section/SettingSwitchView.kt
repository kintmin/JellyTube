package com.kintmin.presentation.ui.setting.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray10
import com.kintmin.presentation.theme.gray60

/** 설정 항목의 trailing 토글. 켜짐을 색뿐 아니라 thumb 의 체크로도 보여준다. */
@Composable
fun SettingSwitchView(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = if (checked) {
            {
                Icon(
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
            checkedTrackColor = MaterialTheme.colorScheme.tertiary,
            checkedIconColor = MaterialTheme.colorScheme.tertiary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = gray60,
            // 기본값은 배경과 같은 색이라 꺼진 토글이 구멍처럼 보인다
            uncheckedTrackColor = gray10,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Preview
@Composable
fun SettingSwitchViewPreview() {
    JellyTubeTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            SettingSwitchView(checked = true, onCheckedChange = {})
            SettingSwitchView(checked = false, onCheckedChange = {})
        }
    }
}
