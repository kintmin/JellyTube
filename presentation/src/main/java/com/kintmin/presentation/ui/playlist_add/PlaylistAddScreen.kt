package com.kintmin.presentation.ui.playlist_add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemUiState
import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemView

@Composable
fun PlaylistAddScreen(
    navigateToBack: () -> Unit,
) {
    val mainViewModel = hiltViewModel<PlaylistAddViewModel>()

    val audioList by mainViewModel.audioListFlow.collectAsState()
    val checkedItemCount by mainViewModel.checkedItemCountFlow.collectAsState()

    PlaylistAddScreen(
        navigateToBack = navigateToBack,
        dataList = audioList,
        checkedItemCount = checkedItemCount,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistAddScreen(
    navigateToBack: () -> Unit,
    dataList: List<PlaylistAddListItemUiState>,
    checkedItemCount: Int,
    sendIntent: (PlaylistAddIntent) -> Unit,
) {
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "플레이리스트 추가",
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "ArrowBackIosNew"
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RectangleShape,
                    enabled = checkedItemCount > 0,
                    onClick = { sendIntent(PlaylistAddIntent.OnClickAdd) }) {
                    Text(
                        text = "$checkedItemCount 추가하기",
                    )
                }
                Box(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(4))
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp, end = 4.dp),
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search"
                )
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .padding(vertical = 16.dp),
                    maxLines = 1,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    value = searchText,
                    onValueChange = { newText ->
                        searchText = newText
                        sendIntent(PlaylistAddIntent.OnChangeSearchText(newText))
                    },
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                items(
                    count = dataList.size,
                    key = { index -> dataList[index].id },
                ) { index ->
                    PlaylistAddListItemView(
                        modifier = Modifier.height(56.dp),
                        data = dataList[index],
                        sendIntent = sendIntent,
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlaylistAddScreenPreview() {
    JellyTubeTheme {
        PlaylistAddScreen(
            navigateToBack = {},
            dataList = PlaylistAddListItemUiState.getMockList(),
            checkedItemCount = PlaylistAddListItemUiState.getMockList().count { it.isChecked },
            sendIntent = {},
        )
    }
}