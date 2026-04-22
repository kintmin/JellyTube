package com.kintmin.jellytube

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import com.kintmin.platform.service_controller.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
    private val registerUserUseCase: RegisterUserUseCase,
) : ViewModel() {

    private val deepLinkChannel = Channel<Uri>(capacity = Channel.BUFFERED)
    val deepLinkFlow = deepLinkChannel.receiveAsFlow()

    fun registerUser() {
        viewModelScope.launch {
            registerUserUseCase()
        }
    }

    fun initializeMediaController() {
        mediaControllerManager.initialize()
    }

    fun releaseMediaController() {
        mediaControllerManager.release()
    }

    fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        intent.data = null  // 사용된 딥링크는 소비
        viewModelScope.launch {
            deepLinkChannel.send(uri)
        }
    }
}
