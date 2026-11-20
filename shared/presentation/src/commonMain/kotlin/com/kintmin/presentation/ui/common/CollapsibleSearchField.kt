package com.kintmin.presentation.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarIconSize

private val SearchFieldAnimation = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)

/**
 * 상단 바의 [SearchToggleButton] 으로 여닫는 검색 필드. 닫을 때는 [onClose] 가 검색어도 비운다.
 *
 * 상단 바 안에 두지 않는다 — innerPadding 이 늘어 목록이 통째로 밀린다.
 */
@Composable
fun CollapsibleSearchField(
    modifier: Modifier = Modifier,
    visible: Boolean,
    value: String,
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.background,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
            fadeIn(animationSpec = SearchFieldAnimation),
        exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
            fadeOut(animationSpec = SearchFieldAnimation),
    ) {
        // AnimatedVisibility 안에서 요청한다. 바깥에서 visible 로 걸면 필드가 아직 붙지 않은
        // 시점에 돌 수 있다.
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .background(containerColor)
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                val isEmpty = value.isEmpty()
                // 비어 있으면 지울 것이 없다. 그때는 닫는 것이 유일하게 뜻이 있다.
                IconButton(onClick = { if (isEmpty) onClose() else onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = if (isEmpty) "검색 닫기" else "검색어 전체 삭제",
                    )
                }
            },
        )
    }
}

/** 열림 여부와 상관없이 아이콘은 돋보기다. 필드에도 X 가 있어 둘이 같으면 무엇을 닫는지 읽히지 않는다. */
@Composable
fun SearchToggleButton(
    isSearchFieldVisible: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.Search,
            modifier = Modifier.size(topAppBarIconSize),
            contentDescription = if (isSearchFieldVisible) "검색 닫기" else contentDescription,
        )
    }
}

@Preview
@Composable
private fun SearchToggleButtonPreview() {
    JellyTubeTheme {
        SearchToggleButton(isSearchFieldVisible = false, contentDescription = "곡 검색", onClick = {})
    }
}

@Preview
@Composable
private fun CollapsibleSearchFieldPreview() {
    JellyTubeTheme {
        CollapsibleSearchField(
            visible = true,
            value = "밤편지",
            label = "곡 이름 검색",
            onValueChange = {},
            onClose = {},
        )
    }
}
