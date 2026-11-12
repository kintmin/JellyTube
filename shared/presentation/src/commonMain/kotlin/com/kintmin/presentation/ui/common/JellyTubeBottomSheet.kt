package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.platform.PlatformBottomSheetSystemBarEffect
import com.kintmin.presentation.theme.JellyTubeTheme

/** 중간 높이에 멈추지 않는다 — 멈추면 같은 시트가 내용 길이에 따라 반만 뜨기도 한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JellyTubeBottomSheet(
    onDismissRequest: () -> Unit,
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        PlatformBottomSheetSystemBarEffect(lightNavigationBarIcons = false)
        content()
    }
}

@Preview
@Composable
private fun JellyTubeBottomSheetPreview() {
    JellyTubeTheme {
        JellyTubeBottomSheet(onDismissRequest = {}) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "바텀시트 내용",
            )
        }
    }
}
