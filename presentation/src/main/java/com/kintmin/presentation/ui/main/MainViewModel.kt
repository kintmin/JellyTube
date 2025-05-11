package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _currentTabItem = MutableStateFlow(savedStateHandle.toRoute<MainScreenRoute>().tabItem)
    val tabItem = _currentTabItem.asStateFlow()

    fun sendIntent(intent: MainScreenIntent) {
        when (intent) {
            is MainScreenIntent.ChangeTab -> updateTabItem(intent.tab)
        }
    }

    private fun updateTabItem(newTabItem: MainTabItem) {
        _currentTabItem.update { newTabItem }
    }
}
