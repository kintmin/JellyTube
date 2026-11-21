package com.kintmin.presentation.ui.audio_edit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
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
import com.kintmin.presentation.theme.seaBlue100
import com.kintmin.presentation.ui.common.JellyTubeDialog

@Composable
fun AudioEditGuideDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = isShow,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
        ) {
            Text(
                modifier = Modifier.padding(vertical = 12.dp),
                text = "이렇게 편집합니다",
                style = MaterialTheme.typography.titleLarge,
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GuideRow(icon = Icons.Rounded.TouchApp, text = "조각을 눌러 고르면 양 끝 손잡이를 끌어 길이를 바꿉니다")
                GuideRow(icon = Icons.Rounded.DragIndicator, text = "고른 조각 가운데 표시를 꾹 눌러 끌면 조각을 옮깁니다")
                GuideRow(icon = Icons.Rounded.ContentCut, text = "위 눈금을 눌러 흰 기준선을 옮기고 그 자리에서 자릅니다")
                GuideRow(icon = Icons.Rounded.Pinch, text = "두 손가락으로 벌리거나 좁혀 확대·축소합니다")
            }
            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 24.dp, bottom = 8.dp),
                onClick = onDismiss,
            ) {
                Text(text = "알겠어요")
            }
        }
    }
}

@Composable
private fun GuideRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = icon,
            contentDescription = null,
            tint = seaBlue100,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun AudioEditGuideDialogPreview() {
    JellyTubeTheme {
        AudioEditGuideDialog(
            isShow = true,
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun GuideRowPreview() {
    JellyTubeTheme {
        GuideRow(icon = Icons.Rounded.DragIndicator, text = "고른 조각 가운데 표시를 꾹 눌러 끌면 조각을 옮깁니다")
    }
}
