package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.selectedFill
import com.kintmin.presentation.ui.common.JellyTubeDialog
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDateTime

@Composable
fun SleepTimerAnnouncementDialog(
    showDialog: Boolean,
    selectedMediaId: Int?,
    selectableMediaList: List<AudioMedia>,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    JellyTubeDialog(showDialog = showDialog, onDismiss = onDismiss) {
        PlaybackDialogColumn(title = "마무리 음원") {
            Text(
                text = "30초 이내이고 길이를 아는 음원만 고를 수 있습니다",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    SleepTimerAnnouncementRow(
                        name = "없음",
                        description = "마무리 음원 없이 멈춥니다",
                        isSelected = selectedMediaId == null,
                        onClick = { onSelect(null) },
                    )
                }
                items(selectableMediaList, key = { it.id }) { media ->
                    SleepTimerAnnouncementRow(
                        name = media.name,
                        description = media.playbackDuration?.to_hh_colon_mm_colon_ss().orEmpty(),
                        isSelected = selectedMediaId == media.id,
                        onClick = { onSelect(media.id) },
                    )
                }
                if (selectableMediaList.isEmpty()) {
                    item {
                        Text(
                            text = "고를 수 있는 음원이 없습니다. 음원 편집으로 30초 이내의 음원을 만들어 주세요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerAnnouncementRow(
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // 고른 행은 체크만이 아니라 채움으로도 드러낸다. 작은 체크 하나로는 어느 줄인지 잘 안 보인다.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // selectable 보다 먼저 잘라야 눌림 표시도 둥글게 번진다.
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isSelected) selectedFill else Color.Transparent,
            )
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isSelected) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.Check,
                contentDescription = "고름",
                // primary 는 surface 대비가 2.6:1 이라 작은 아이콘에 필요한 3:1 에 못 미친다.
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun SleepTimerAnnouncementDialogPreview() {
    JellyTubeTheme {
        SleepTimerAnnouncementDialog(
            showDialog = true,
            selectedMediaId = 2,
            selectableMediaList = listOf(
                previewAnnouncement(id = 1, name = "잘 자요"),
                previewAnnouncement(id = 2, name = "좋은 꿈 꾸세요"),
            ),
            onDismiss = {},
            onSelect = {},
        )
    }
}

@Preview
@Composable
private fun SleepTimerAnnouncementDialogEmptyPreview() {
    JellyTubeTheme {
        SleepTimerAnnouncementDialog(
            showDialog = true,
            selectedMediaId = null,
            selectableMediaList = emptyList(),
            onDismiss = {},
            onSelect = {},
        )
    }
}

private fun previewAnnouncement(id: Int, name: String) = AudioMedia(
    id = id,
    source = "source",
    name = name,
    artist = "아티스트",
    description = "",
    audioDuration = 12.seconds,
    audioFileFullPath = "/audio/$id.m4a",
    imageFileFullPath = null,
    createdTime = LocalDateTime(2026, 11, 19, 0, 0),
)
