package com.kintmin.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60

/** 행 전체가 탭을 받으므로 표시만 하고 따로 눌리지 않는다. */
@Composable
fun SelectionCheckIndicator(
    isChecked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .semantics { contentDescription = if (isChecked) "선택됨" else "선택 안 됨" },
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                modifier = Modifier
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(3.dp),
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(width = 1.5.dp, color = gray60, shape = CircleShape),
            )
        }
    }
}

@Preview
@Composable
fun SelectionCheckIndicatorPreview() {
    JellyTubeTheme {
        Row(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            SelectionCheckIndicator(isChecked = false)
            SelectionCheckIndicator(isChecked = true)
        }
    }
}
