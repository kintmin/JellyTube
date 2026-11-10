package com.kintmin.presentation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.writeToFile
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContext
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImagePNGRepresentation
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGSizeMake
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalForeignApi::class)
class DominantColorTest {

    @Test
    fun `단색 이미지는 그 색을 돌려준다`() = runTest {
        val path = writeSolidColorPng(UIColor.redColor, "dominant-red.png")

        assertEquals(0xFFFF0000.toInt(), extractDominantColor(path))
    }

    @Test
    fun `면적이 넓은 쪽이 이긴다`() = runTest {
        // 아래 3/4 를 파랑으로 덮는다. 최빈 묶음이 바뀌어야 한다.
        val path = writeTwoTonePng("dominant-two-tone.png")

        assertEquals(0xFF0000FF.toInt(), extractDominantColor(path))
    }

    @Test
    fun `없는 파일은 null 이다`() = runTest {
        assertNull(extractDominantColor(NSTemporaryDirectory() + "없는파일.png"))
    }

    private fun writeSolidColorPng(color: UIColor, fileName: String): String =
        writePng(fileName) { context ->
            CGContextSetFillColorWithColor(context, color.CGColor)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, 64.0, 64.0))
        }

    private fun writeTwoTonePng(fileName: String): String =
        writePng(fileName) { context ->
            CGContextSetFillColorWithColor(context, UIColor.redColor.CGColor)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, 64.0, 16.0))
            CGContextSetFillColorWithColor(context, UIColor.blueColor.CGColor)
            CGContextFillRect(context, CGRectMake(0.0, 16.0, 64.0, 48.0))
        }

    private fun writePng(fileName: String, draw: (platform.CoreGraphics.CGContextRef?) -> Unit): String {
        UIGraphicsBeginImageContext(CGSizeMake(64.0, 64.0))
        draw(UIGraphicsGetCurrentContext())
        val image = requireNotNull(UIGraphicsGetImageFromCurrentImageContext())
        UIGraphicsEndImageContext()

        val path = NSTemporaryDirectory() + fileName
        requireNotNull(UIImagePNGRepresentation(image)).writeToFile(path, atomically = true)
        return path
    }
}
