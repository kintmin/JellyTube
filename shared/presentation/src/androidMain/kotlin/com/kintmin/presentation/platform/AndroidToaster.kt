package com.kintmin.presentation.platform

import android.content.Context
import android.widget.Toast

class AndroidToaster(
    private val appContext: Context,
) : Toaster {

    override fun show(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLong(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
    }
}
