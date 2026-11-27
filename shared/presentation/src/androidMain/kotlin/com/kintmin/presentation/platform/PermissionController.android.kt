package com.kintmin.presentation.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * OS 버전에 따라 실제로 필요한 권한 문자열. 필요 없으면 null.
 *
 * 저장 권한은 SDK 28 이하는 WRITE, 32 이하는 READ 가 필요하고 그 위는 불필요하다.
 * 알림은 33 이상에서만, 신체 활동 인식은 29 이상에서만 필요하다.
 */
private fun AppPermission.androidPermissionOrNull(): String? = when (this) {
    AppPermission.Notification ->
        Manifest.permission.POST_NOTIFICATIONS
            .takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }

    AppPermission.MediaStorage -> when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> Manifest.permission.READ_EXTERNAL_STORAGE
        else -> null
    }

    AppPermission.ActivityRecognition ->
        Manifest.permission.ACTIVITY_RECOGNITION
            .takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q }
}

private class AndroidPermissionController(
    private val context: Context,
    private val requestPermissions: (Array<String>) -> Unit,
    private val setPendingResult: ((Map<AppPermission, Boolean>) -> Unit) -> Unit,
) : PermissionController {

    override fun isGranted(permission: AppPermission): Boolean {
        val androidPermission = permission.androidPermissionOrNull() ?: return true
        return context.checkSelfPermission(androidPermission) == PackageManager.PERMISSION_GRANTED
    }

    override fun request(
        permissions: List<AppPermission>,
        onResult: (Map<AppPermission, Boolean>) -> Unit,
    ) {
        val notGranted = permissions.filterNot { isGranted(it) }
        if (notGranted.isEmpty()) return

        val androidPermissions = notGranted.mapNotNull { it.androidPermissionOrNull() }
        if (androidPermissions.isEmpty()) return

        setPendingResult { granted ->
            onResult(notGranted.associateWith { granted[it] ?: isGranted(it) })
        }
        requestPermissions(androidPermissions.toTypedArray())
    }
}

@Composable
actual fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    // 요청 중인 콜백을 담아둔다. 런처 결과는 권한 문자열 기준으로 오므로
    // AppPermission 으로 되돌리지 않고 isGranted 를 다시 물어 답을 만든다.
    val pendingResult = remember { arrayOfNulls<(Map<AppPermission, Boolean>) -> Unit>(1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        val callback = pendingResult[0] ?: return@rememberLauncherForActivityResult
        pendingResult[0] = null
        callback(emptyMap())
    }

    return remember(context, launcher) {
        AndroidPermissionController(
            context = context,
            requestPermissions = { launcher.launch(it) },
            setPendingResult = { pendingResult[0] = it },
        )
    }
}
