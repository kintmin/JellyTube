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
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.accentDark
import com.kintmin.presentation.theme.emptyTrack

@Composable
fun SettingSwitchView(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = accentDark,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = contentMuted,
            uncheckedTrackColor = emptyTrack,
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
