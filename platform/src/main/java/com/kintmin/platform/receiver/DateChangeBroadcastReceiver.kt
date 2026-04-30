package com.kintmin.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateChangeBroadcastReceiver: BroadcastReceiver() {

    private var onDateChangedListener: OnDateChangedListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_DATE_CHANGED -> {
                onDateChangedListener?.onDateChanged()
            }
            Intent.ACTION_TIME_CHANGED -> {
                onDateChangedListener?.onTimeChanged()
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val timeZone = intent.getStringExtra("time-zone")
                onDateChangedListener?.onTimezoneChanged(timeZone ?: "")
            }
        }
    }

    fun setOnDateChangedListener(onDateChangedListener: OnDateChangedListener) {
        this.onDateChangedListener = onDateChangedListener
    }

    interface OnDateChangedListener {
        fun onDateChanged()
        fun onTimeChanged()
        fun onTimezoneChanged(timeZone: String)
    }
}