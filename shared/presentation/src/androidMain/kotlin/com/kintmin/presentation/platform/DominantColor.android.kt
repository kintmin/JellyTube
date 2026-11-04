package com.kintmin.presentation.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 대표 색을 못 뽑았을 때 Palette 에 넘기는 기본값. 앱의 기본 그라데이션 최상단 색이다. */
private const val FALLBACK_DOMINANT_COLOR = 0xFF4B453B.toInt()

/** 색만 필요하므로 원본을 그대로 올리지 않고 240x240 가량으로 다운샘플한다. */
private fun calculateInSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int = 240,
    reqHeight: Int = 240,
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

actual suspend fun extractDominantColor(imageFileFullPath: String): Int? =
    withContext(Dispatchers.IO) {
        if (!File(imageFileFullPath).exists()) return@withContext null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFileFullPath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val sampled = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeFile(imageFileFullPath, sampled) ?: return@withContext null

        runCatching {
            Palette.Builder(bitmap)
                .maximumColorCount(16)
                .generate()
                .getDominantColor(FALLBACK_DOMINANT_COLOR)
        }.getOrNull().also {
            bitmap.recycle()
        }
    }
