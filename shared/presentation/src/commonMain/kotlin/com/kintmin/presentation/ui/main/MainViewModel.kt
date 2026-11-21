package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.main.navigation.mainScreenTypeMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MainUiState(tabItem = savedStateHandle.toRoute<MainScreenRoute>(mainScreenTypeMap).tabItem)
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun sendIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.ChangeTab -> updateTabItem(intent.tab)
        }
    }

    private fun updateTabItem(newTabItem: MainTabItem) {
        _uiState.update { it.copy(tabItem = newTabItem) }
    }
}
