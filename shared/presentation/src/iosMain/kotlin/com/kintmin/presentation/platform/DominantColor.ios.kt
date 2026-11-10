package com.kintmin.presentation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIImage

/** 대표 색을 못 뽑았을 때의 기본값. Android 와 같은 값 — 앱 기본 그라데이션 최상단 색이다. */
private const val FALLBACK_DOMINANT_COLOR = 0xFF4B453B.toInt()

/** 색만 필요하므로 원본 크기와 무관하게 이 크기로 줄여 그린다. Android 의 다운샘플과 같은 뜻이다. */
private const val SAMPLE_SIZE = 32

/** 색 묶는 단위. 채널당 상위 5비트(32단계) — Palette 의 양자화와 같은 거칠기다. */
private const val QUANTIZE_SHIFT = 3

/** 거의 투명한 픽셀은 배경이라 센다고 의미가 없다. */
private const val MIN_ALPHA = 128

/**
 * 대표 색을 뽑는다.
 *
 * Android 가 Palette 로 하는 일을 직접 한다 — 작게 줄여 그리고, 색을 거칠게 묶어 가장 많은
 * 묶음을 고르고, 그 묶음의 평균색을 돌려준다. Palette 의 `getDominantColor` 가 "가장 인구가
 * 많은 swatch" 인 것과 같은 정의다.
 *
 * 실패하면 null 이다. 호출자가 null 을 기본 그라데이션으로 처리한다.
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun extractDominantColor(imageFileFullPath: String): Int? =
    withContext(Dispatchers.Default) {
        val cgImage = UIImage.imageWithContentsOfFile(imageFileFullPath)?.CGImage
            ?: return@withContext null

        memScoped {
            val pixels = allocArray<UByteVar>(SAMPLE_SIZE * SAMPLE_SIZE * BYTES_PER_PIXEL)
            val colorSpace = CGColorSpaceCreateDeviceRGB()
            val context = CGBitmapContextCreate(
                data = pixels,
                width = SAMPLE_SIZE.convert(),
                height = SAMPLE_SIZE.convert(),
                bitsPerComponent = 8.convert(),
                bytesPerRow = (SAMPLE_SIZE * BYTES_PER_PIXEL).convert(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
            )
            CGColorSpaceRelease(colorSpace)
            context ?: return@withContext null

            CGContextDrawImage(
                context,
                CGRectMake(0.0, 0.0, SAMPLE_SIZE.toDouble(), SAMPLE_SIZE.toDouble()),
                cgImage,
            )
            CGContextRelease(context)

            // 묶음별로 개수와 채널 합을 함께 들고 간다 — 고른 뒤 평균색을 내야 해서다.
            val counts = HashMap<Int, IntArray>()
            var best: IntArray? = null
            var bestCount = 0

            for (index in 0 until SAMPLE_SIZE * SAMPLE_SIZE) {
                val offset = index * BYTES_PER_PIXEL
                if (pixels[offset + 3].toInt() < MIN_ALPHA) continue

                val red = pixels[offset].toInt()
                val green = pixels[offset + 1].toInt()
                val blue = pixels[offset + 2].toInt()
                val key = (red shr QUANTIZE_SHIFT shl 10) or
                    (green shr QUANTIZE_SHIFT shl 5) or
                    (blue shr QUANTIZE_SHIFT)

                val bucket = counts.getOrPut(key) { IntArray(4) }
                bucket[0]++
                bucket[1] += red
                bucket[2] += green
                bucket[3] += blue

                if (bucket[0] > bestCount) {
                    bestCount = bucket[0]
                    best = bucket
                }
            }

            val bucket = best ?: return@withContext FALLBACK_DOMINANT_COLOR
            val count = bucket[0]
            (0xFF shl 24) or
                ((bucket[1] / count) shl 16) or
                ((bucket[2] / count) shl 8) or
                (bucket[3] / count)
        }
    }

private const val BYTES_PER_PIXEL = 4
