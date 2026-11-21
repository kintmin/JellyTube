package com.kintmin.presentation.ui.main.playlist

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentDisabled
import com.kintmin.presentation.theme.contentSubtle
import com.kintmin.presentation.ui.common.SectionHeader
import com.kintmin.presentation.ui.main.playlist.dialog.PlaylistAddDialog
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistAddButtonView
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistItemView
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistLibraryBlockView

private const val REVEAL_MILLIS = 300
private val RevealOffset = 12.dp

@Composable
fun PlaylistView(
    modifier: Modifier,
    data: List<PlaylistItemUiState>,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    val libraryItems = data.filter { it.isBasePlaylist }
    val userItems = data.filterNot { it.isBasePlaylist }
    // 입구가 둘(머리말·빈 상태)이라 다이얼로그는 여기 한 벌만 둔다.
    var showAddDialog by remember { mutableStateOf(false) }

    // 목록이 비동기로 도착하는 순간 툭 나타나지 않게 한다.
    val isLoaded = libraryItems.isNotEmpty()
    val revealProgress by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(REVEAL_MILLIS, easing = LinearOutSlowInEasing),
    )
    val revealOffsetPx = with(LocalDensity.current) { RevealOffset.toPx() }

    PlaylistAddDialog(
        showDialog = showAddDialog,
        onDismiss = { showAddDialog = false },
        existingNames = data.map { it.name },
        makePlaylist = { title -> sendIntent(PlaylistIntent.MakeNewPlaylist(title)) },
    )

    LazyVerticalGrid(
        modifier = modifier.graphicsLayer {
            alpha = revealProgress
            translationY = (1f - revealProgress) * revealOffsetPx
        },
        columns = GridCells.Fixed(2),
        // 좌우 여백을 격자에 주지 않는다. 라이브러리 행의 ink 가 가장자리까지 닿아야 한다.
        // 가장자리 여백은 각 항목이 자기 안쪽 패딩으로 만든다.
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (libraryItems.isNotEmpty()) {
            // 머리말을 붙이지 않는다. 아래 머리말과 구분선이 이미 둘을 가르고,
            // 한 줄을 비워야 전체 항목이 처음 화면에 들어온다.
            item(key = "library-block", span = { GridItemSpan(maxLineSpan) }) {
                PlaylistLibraryBlockView(
                    modifier = Modifier.fillMaxWidth(),
                    data = libraryItems,
                    sendIntent = sendIntent,
                )
            }
        }

        item(key = "user-header", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                text = "내 플레이리스트",
                showDivider = libraryItems.isNotEmpty(),
                action = {
                    // 빈 상태가 제 CTA 를 갖는다. 같은 명령으로 가는 버튼을 둘 보이지 않게 한다.
                    if (userItems.isEmpty()) return@SectionHeader
                    // 바꿀 것이 없으면 빈 시트가 올라온다. 누를 수 없게 막는다.
                    val canReorder = userItems.size > 1
                    // 색을 한 단계 낮춰 만들기가 주, 순서가 보조로 읽히게 한다.
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = canReorder,
                                onClick = { sendIntent(PlaylistIntent.OnClickShowReorderBottomSheet) },
                            )
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val reorderColor = if (canReorder) contentSubtle else contentDisabled
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = null,
                            tint = reorderColor,
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = "순서",
                            style = MaterialTheme.typography.labelLarge,
                            color = reorderColor,
                        )
                    }
                    PlaylistAddButtonView(text = "새로 만들기", onClick = { showAddDialog = true })
                },
            )
        }

        // 라이브러리가 비어 있다는 것은 아직 안 읽혔다는 뜻이다 — 시스템 플레이리스트 셋은
        // 앱 시작 때 보장된다. 그것으로 가르지 않으면 첫 프레임마다 빈 상태가 깜박인다.
        if (userItems.isEmpty() && libraryItems.isNotEmpty()) {
            item(key = "user-empty", span = { GridItemSpan(maxLineSpan) }) {
                PlaylistEmptyView(
                    modifier = Modifier,
                    onClickAdd = { showAddDialog = true },
                )
            }
        } else {
            items(
                count = userItems.size,
                key = { index -> userItems[index].id }
            ) { index ->
                PlaylistItemView(
                    modifier = Modifier,
                    data = userItems[index],
                    sendIntent = sendIntent,
                )
            }
        }
    }
}

@Preview
@Composable
fun PlaylistPreview() {
    JellyTubeTheme {
        PlaylistView(
            modifier = Modifier.fillMaxSize(),
            PlaylistItemUiState.getMockList(),
            sendIntent = {},
        )
    }
}
