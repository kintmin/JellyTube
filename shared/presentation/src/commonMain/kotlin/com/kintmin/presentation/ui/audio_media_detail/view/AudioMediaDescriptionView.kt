package com.kintmin.presentation.ui.audio_media_detail.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.accentContent
import com.kintmin.presentation.theme.contentMuted

private const val COLLAPSED_LINE_COUNT = 4

/**
 * 음원 설명. 길이 제한이 5000자라 접어서 보여주고 [COLLAPSED_LINE_COUNT] 줄을 넘을 때만 펼칠 수 있다.
 */
@Composable
fun AudioMediaDescriptionView(
    description: String,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember(description) { mutableStateOf(false) }
    var isOverflowing by remember(description) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "설명",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = contentMuted,
        )
        // 선택 핸들을 끄는 제스처가 클릭으로 읽히지 않도록 펼치기 버튼은 이 컨테이너 밖에 둔다.
        SelectionContainer {
            // 여러 줄 단락이라 라벨과 줄 간격을 한 줄 메타 값보다 넉넉하게 둔다.
            Text(
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                text = description,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_LINE_COUNT,
                overflow = TextOverflow.Ellipsis,
                // 펼친 뒤에는 넘치지 않으므로 그때 갱신하면 접는 길이 사라진다.
                onTextLayout = { result -> if (!isExpanded) isOverflowing = result.hasVisualOverflow },
            )
        }
        if (isOverflowing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isExpanded) "접기" else "더 보기",
                    fontSize = 14.sp,
                    color = accentContent,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentContent,
                )
            }
        }
    }
}

@Preview
@Composable
fun AudioMediaDescriptionViewPreview() {
    JellyTubeTheme {
        AudioMediaDescriptionView(
            description = List(12) { "이것은 최대 5000자까지 가능한 미디어 설명입니다." }.joinToString(" "),
        )
    }
}

@Preview
@Composable
fun AudioMediaDescriptionViewShortPreview() {
    JellyTubeTheme {
        AudioMediaDescriptionView(description = "짧은 설명입니다.")
    }
}
