package com.kintmin.ios.download

import platform.Foundation.NSUserDefaults

internal class IosDownloadTargetStore {

    fun save(url: String, playlistId: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(playlistId.toLong(), key(url))
    }

    fun get(url: String): Int? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = key(url)
        if (defaults.objectForKey(key) == null) return null
        return defaults.integerForKey(key).toInt()
    }

    fun remove(url: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key(url))
    }

    private fun key(url: String) = "$KEY_PREFIX$url"

    private companion object {
        const val KEY_PREFIX = "youtubeDownload.playlistId."
    }
}
