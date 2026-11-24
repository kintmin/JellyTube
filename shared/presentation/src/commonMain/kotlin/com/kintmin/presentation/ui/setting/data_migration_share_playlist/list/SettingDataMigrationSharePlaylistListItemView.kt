package com.kintmin.presentation.ui.setting.data_migration_share_playlist.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.ui.common.SelectionCheckIndicator

@Composable
fun SettingDataMigrationSharePlaylistListItemView(
    modifier: Modifier,
    data: SettingDataMigrationSharePlaylistListItemUiState,
    onClick: () -> Unit,
) {
    Card(modifier, shape = RectangleShape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionCheckIndicator(isChecked = data.isChecked)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (data.isSecret) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "비밀 플레이리스트",
                            modifier = Modifier.size(14.dp),
                            tint = contentMuted,
                        )
                    }
                    Text(
                        text = data.name,
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = data.subTitle,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    color = contentMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingDataMigrationSharePlaylistListItemViewPreview() {
    JellyTubeTheme {
        SettingDataMigrationSharePlaylistListItemView(
            modifier = Modifier.height(56.dp),
            data = SettingDataMigrationSharePlaylistListItemUiState.getMock(),
            onClick = {},
        )
    }
}
