package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.gray80
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState

private val ROW_HEIGHT = 56.dp

/**
 * 시스템 플레이리스트를 묶어 그리는 한 덩어리.
 *
 * 사용자 플레이리스트와 **다른 컴포넌트로** 그린다. 성격이 다르기 때문이다 — 지울 수 없고,
 * 순서를 바꿀 수 없고, 곡을 직접 넣는 곳도 아니다. 같은 카드로 그리면 그 차이가 점 세 개
 * 메뉴를 열어야만 드러난다.
 *
 * 썸네일도 쓰지 않는다. 데이터 계층이 첫 곡의 앨범 아트를 자동으로 넣기 때문에, 카드로 그리면
 * "전체" 에 임의의 앨범 커버가 박히고 첫 곡이 같은 사용자 플레이리스트와 같은 그림이 된다.
 */
@Composable
fun PlaylistLibraryBlockView(
    modifier: Modifier,
    data: List<PlaylistItemUiState>,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    // 틀을 두르지 않는다. 설정 화면처럼 배경 위에 행을 그대로 쌓는다.
    Column(modifier = modifier) {
        data.forEach { item ->
            PlaylistLibraryRowView(
                data = item,
                sendIntent = sendIntent,
            )
        }
    }
}

@Composable
private fun PlaylistLibraryRowView(
    data: PlaylistItemUiState,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            // ink 는 가장자리까지, 내용만 안쪽으로. 설정 화면 항목과 같다.
            .clickable { sendIntent(PlaylistIntent.OnClickPlaylistItem(data)) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = data.type.libraryIcon(),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = gray80,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            text = data.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = data.countString,
            style = MaterialTheme.typography.bodySmall,
            color = gray60,
            maxLines = 1,
        )
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(20.dp),
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = gray60,
        )
    }
}

private fun PlaylistType.libraryIcon(): ImageVector = when (this) {
    PlaylistType.TOTAL -> Icons.Rounded.LibraryMusic
    PlaylistType.FAVORITE -> Icons.Rounded.Favorite
    PlaylistType.UNCATEGORIZED -> Icons.Rounded.Folder
    PlaylistType.USER -> Icons.Rounded.LibraryMusic
}

@Preview
@Composable
fun PlaylistLibraryBlockPreview() {
    JellyTubeTheme {
        PlaylistLibraryBlockView(
            modifier = Modifier,
            data = listOf(
                PlaylistItemUiState.getMock(id = 1, type = PlaylistType.TOTAL).copy(name = "전체"),
                PlaylistItemUiState.getMock(id = 2, type = PlaylistType.FAVORITE).copy(name = "즐겨찾기", audioMediaCount = 12),
                PlaylistItemUiState.getMock(id = 3, type = PlaylistType.UNCATEGORIZED).copy(name = "미분류", audioMediaCount = 0),
            ),
            sendIntent = {},
        )
    }
}
