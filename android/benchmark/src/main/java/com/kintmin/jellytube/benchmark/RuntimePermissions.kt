package com.kintmin.jellytube.benchmark

import android.Manifest
import androidx.benchmark.macro.MacrobenchmarkScope

// 측정 빌드는 실행마다 새로 깔려 권한이 비어 있다. 비어 있으면 첫 화면을 권한 창이 덮고 걸음 서비스가 뜨지 않는다.
private val RUNTIME_PERMISSIONS = listOf(
    Manifest.permission.POST_NOTIFICATIONS,
    Manifest.permission.ACTIVITY_RECOGNITION,
)

fun MacrobenchmarkScope.grantRuntimePermissions() {
    RUNTIME_PERMISSIONS.forEach { device.executeShellCommand("pm grant $packageName $it") }
}
