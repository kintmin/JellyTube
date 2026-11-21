package com.kintmin.presentation.ui.playlist_add

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.CollapsibleSearchField
import com.kintmin.presentation.ui.common.FloatingSearchFieldHeight
import com.kintmin.presentation.ui.common.SearchToggleButton
import com.kintmin.presentation.ui.common.dragToSelect
import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemUiState
import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistAddScreen(
    navigateToBack: () -> Unit,
) {
    val mainViewModel = koinViewModel<PlaylistAddViewModel>()

    val uiState by mainViewModel.uiState.collectAsState()
    val toaster = rememberToaster()

    LaunchedEffect(Unit) {
        mainViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistAddEvent.NavigateToBack -> navigateToBack()
                is PlaylistAddEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    PlaylistAddScreen(
        navigateToBack = navigateToBack,
        playlistName = uiState.playlistName,
        dataList = uiState.audioList,
        checkedItemCount = uiState.checkedItemCount,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistAddScreen(
    navigateToBack: () -> Unit,
    playlistName: String,
    dataList: List<PlaylistAddListItemUiState>?,
    checkedItemCount: Int,
    sendIntent: (PlaylistAddIntent) -> Unit,
) {
    var isSearchFieldVisible by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    fun changeSearchText(text: String) {
        searchText = text
        sendIntent(PlaylistAddIntent.OnChangeSearchText(text))
    }
    fun closeSearchField() {
        isSearchFieldVisible = false
        changeSearchText("")
    }
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    // 이름을 제목에 붙이면 길 때 "곡 추가" 가 말줄임에 잘린다.
                    Column {
                        Text(
                            text = "곡 추가",
                            fontSize = topAppBarDetailTitleFontSize,
                            lineHeight = topAppBarDetailTitleLineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "뒤로"
                        )
                    }
                },
                actions = {
                    SearchToggleButton(
                        isSearchFieldVisible = isSearchFieldVisible,
                        contentDescription = "곡 검색",
                        onClick = {
                            if (isSearchFieldVisible) closeSearchField() else isSearchFieldVisible = true
                        },
                    )
                },
            )
        },
        bottomBar = {
            if (checkedItemCount > 0) {
                BottomActionBar(modifier = Modifier.imePadding()) {
                    BottomActionButton(
                        text = "${checkedItemCount}곡 추가",
                        onClick = { sendIntent(PlaylistAddIntent.OnClickAdd) },
                    )
                }
            }
        },
    ) { innerPadding ->
        // 필드가 목록 위에 뜨므로 그만큼 목록 머리를 비워 첫 곡이 가려지지 않게 한다.
        val searchFieldInset by animateDpAsState(
            targetValue = if (isSearchFieldVisible) FloatingSearchFieldHeight else 0.dp,
            label = "searchFieldInset",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (dataList?.isEmpty() == true) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 48.dp)
                            .padding(top = searchFieldInset),
                        text = if (searchText.isEmpty()) {
                            "추가할 수 있는 곡이 없습니다.\n이미 모든 곡이 이 플레이리스트에 있습니다."
                        } else {
                            "검색어와 일치하는 곡이 없습니다."
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val items = dataList.orEmpty()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .dragToSelect(
                            listState = listState,
                            selectableIds = items.map { it.id },
                            checkedIds = items.filter { it.isChecked }.map { it.id }.toSet(),
                            onCheckedIdsChange = { sendIntent(PlaylistAddIntent.OnDragSelectItems(it)) },
                        ),
                    state = listState,
                    contentPadding = PaddingValues(
                        top = searchFieldInset,
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
                ) {
                    items(
                        count = items.size,
                        key = { index -> items[index].id },
                    ) { index ->
                        PlaylistAddListItemView(
                            modifier = Modifier.height(56.dp),
                            data = items[index],
                            sendIntent = sendIntent,
                        )
                    }
                }
            }
            CollapsibleSearchField(
                visible = isSearchFieldVisible,
                value = searchText,
                placeholder = "곡 이름 검색",
                onValueChange = ::changeSearchText,
                onClose = ::closeSearchField,
            )
        }
    }
}


@Preview
@Composable
fun PlaylistAddScreenPreview() {
    JellyTubeTheme {
        PlaylistAddScreen(
            navigateToBack = {},
            playlistName = "드라이브할 때",
            dataList = PlaylistAddListItemUiState.getMockList(),
            checkedItemCount = PlaylistAddListItemUiState.getMockList().count { it.isChecked },
            sendIntent = {},
        )
    }
}
