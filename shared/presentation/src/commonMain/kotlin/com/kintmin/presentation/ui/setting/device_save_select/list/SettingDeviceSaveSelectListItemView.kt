package com.kintmin.presentation.ui.setting.device_save_select.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.AudioArtwork
import com.kintmin.presentation.ui.common.SelectionCheckIndicator
import com.kintmin.presentation.ui.setting.device_save_select.SettingDeviceSaveSelectIntent

@Composable
fun SettingDeviceSaveSelectListItemView(
    modifier: Modifier,
    data: SettingDeviceSaveSelectListItemUiState,
    sendIntent: (SettingDeviceSaveSelectIntent) -> Unit,
) {
    Card(
        modifier,
        shape = RectangleShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sendIntent(SettingDeviceSaveSelectIntent.OnClickAudioItem(data)) },
        ) {
            SelectionCheckIndicator(isChecked = data.isChecked)

            AudioArtwork(
                imageFileFullPath = data.imageFileFullPath,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16))
                    .background(MaterialTheme.colorScheme.surface),
                placeholderIconSize = 16.dp,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = data.mediaName,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = data.subTitle,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
fun SettingDeviceSaveSelectListItemViewPreview() {
    JellyTubeTheme {
        SettingDeviceSaveSelectListItemView(
            modifier = Modifier.height(56.dp),
            data = SettingDeviceSaveSelectListItemUiState.getMock(),
            sendIntent = {},
        )
    }
}
