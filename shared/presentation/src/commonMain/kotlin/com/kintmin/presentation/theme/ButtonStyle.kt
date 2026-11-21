package com.kintmin.presentation.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * 버튼 색은 역할로 고른다. 기본 Button 은 주 동작이고, 나머지는 여기서 고른다.
 * 테두리·글자 버튼의 기본 글자색 primary 는 surface 위에서 대비가 2.6:1 이라 쓰지 않는다.
 */
object JellyTubeButtonColors {
    /** 취소·계속 편집처럼 주 동작 옆의 보조 동작. OutlinedButton 과 쓴다. */
    @Composable
    fun secondary(): ButtonColors =
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)

    /** 상단바 actions 같은 글자 버튼. TextButton 과 쓴다. */
    @Composable
    fun text(): ButtonColors =
        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)

    /** 되돌릴 수 없는 동작 — 지우거나 편집을 버린다. Button 과 쓴다. */
    @Composable
    fun destructive(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )

    /** 되돌릴 수 있는 빼기·해제. OutlinedButton 과 쓴다. */
    @Composable
    fun removal(): ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = destructiveContent)
}

val dialogContentPadding = 24.dp
val dialogTitleSpacing = 16.dp
val dialogActionsSpacing = 24.dp

val dialogButtonSpacing = 8.dp
// 파괴적 버튼과 간격을 벌려 반사적으로 눌리지 않게 한다.
val dialogDestructiveButtonSpacing = 24.dp

val dialogDismissButtonPadding = PaddingValues(horizontal = 24.dp)
val dialogConfirmButtonPadding = PaddingValues(horizontal = 32.dp)
