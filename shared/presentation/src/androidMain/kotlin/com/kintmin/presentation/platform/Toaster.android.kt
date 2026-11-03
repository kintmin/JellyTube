package com.kintmin.presentation.platform

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private class AndroidToaster(
    private val context: Context,
) : Toaster {

    override fun show(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLong(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

@Composable
actual fun rememberToaster(): Toaster {
    val context = LocalContext.current
    return remember(context) { AndroidToaster(context) }
}
