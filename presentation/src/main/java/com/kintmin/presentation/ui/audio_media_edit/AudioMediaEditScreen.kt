package com.kintmin.presentation.ui.audio_media_edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun AudioMediaEditScreen(
    navigateToBack: () -> Unit,
) {
    val mainViewModel = hiltViewModel<AudioMediaEditViewModel>()

}

@Composable
fun AudioMediaEditScreen(
    navigateToBack: () -> Unit,
    data: AudioMediaEditUiState,
) {

}

@Preview(showBackground = true)
@Composable
fun AudioMediaEditScreenPreview() {
    JellyTubeTheme {
        AudioMediaEditScreen(

        )
    }
}