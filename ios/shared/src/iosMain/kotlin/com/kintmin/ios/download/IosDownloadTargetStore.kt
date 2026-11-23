package com.kintmin.ios.download

import platform.Foundation.NSUserDefaults

internal class IosDownloadTargetStore {

    fun save(url: String, playlistId: Int?) {
        NSUserDefaults.standardUserDefaults.setInteger((playlistId ?: NO_PLAYLIST_ID).toLong(), key(url))
    }

    fun get(url: String): Int? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = key(url)
        if (defaults.objectForKey(key) == null) return null
        return defaults.integerForKey(key).toInt().takeUnless { it == NO_PLAYLIST_ID }
    }

    fun remove(url: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key(url))
    }

    private fun key(url: String) = "$KEY_PREFIX$url"

    private companion object {
        const val KEY_PREFIX = "youtubeDownload.playlistId."
        const val NO_PLAYLIST_ID = -1
    }
}
