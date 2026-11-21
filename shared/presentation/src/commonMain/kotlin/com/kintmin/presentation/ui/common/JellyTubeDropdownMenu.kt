package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun JellyTubeDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.medium,
        content = content,
    )
}

@Preview
@Composable
private fun JellyTubeDropdownMenuPreview() {
    JellyTubeTheme {
        JellyTubeDropdownMenu(expanded = true, onDismissRequest = {}) {
            DropdownMenuItem(text = { Text("정보 수정") }, onClick = {})
        }
    }
}
