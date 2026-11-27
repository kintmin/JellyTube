package com.kintmin.presentation.ui.main.notice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme

/**
 * 백업 폴더가 없을 때 한 번 띄운다. 다이얼로그가 아닌 것은 이것이 오류가 아니라 **상태**여서다 —
 * 막을 일이 아니라 알릴 일이고, 첫 화면에 모달을 둘 겹치지 않게 한다.
 */
@Composable
fun BackupNoticeBanner(
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClickLabel = "백업 및 복원 화면으로 이동", role = Role.Button, onClick = onClick)
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "백업할 폴더를 정해 두세요", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "곡 목록과 재생목록을 하루 한 번 저장합니다. 전에 쓰던 백업이 있다면 지금 되돌릴 수도 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "백업 안내 닫기")
            }
        }
    }
}

@Preview
@Composable
private fun BackupNoticeBannerPreview() {
    JellyTubeTheme {
        BackupNoticeBanner(onClick = {}, onDismiss = {})
    }
}
