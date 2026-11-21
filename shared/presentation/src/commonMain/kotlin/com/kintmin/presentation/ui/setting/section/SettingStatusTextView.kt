package com.kintmin.presentation.ui.setting.section

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun SettingStatusTextView(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun SettingStatusTextViewPreview() {
    JellyTubeTheme {
        SettingStatusTextView(text = "보내는 기기를 찾는 중입니다.")
    }
}
