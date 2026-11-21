package com.kintmin.presentation.ui.setting.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun SettingNoticeCardView(
    text: String,
    icon: ImageVector? = null,
    title: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                if (title != null) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                }
                Text(text = text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview
@Composable
private fun SettingNoticeCardViewPreview() {
    JellyTubeTheme {
        SettingNoticeCardView(
            icon = Icons.Rounded.Wifi,
            title = "같은 Wi-Fi 에 연결해 주세요",
            text = "보내는 기기와 받는 기기가 같은 네트워크에 있어야 합니다.",
        )
    }
}
