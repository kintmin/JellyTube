package com.kintmin.presentation.ui.setting.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.kintmin.presentation.theme.seaBlue20
import com.kintmin.presentation.theme.white

@Composable
fun SettingSwitchView(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = white,
            // tertiary 는 재생 모드가 켜졌다는 뜻이라 일반 설정에 쓰지 않는다. primary 는 배경 대비가 3:1 에 못 미친다.
            checkedTrackColor = seaBlue20,
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
