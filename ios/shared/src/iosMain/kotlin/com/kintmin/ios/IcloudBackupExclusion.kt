package com.kintmin.ios

import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * 앱 데이터를 iCloud 백업에서 뺀다. iOS 에는 클라우드 백업과 기기 간 이전을 가르는 API 가 없어
 * **둘 다** 빠진다 — 그렇게 정한 이유는 `docs/DECISIONS.md` 다.
 *
 * 문서 디렉터리 하나에 걸어 하위 전체와 앞으로 생길 것까지 덮는다. 항목마다 걸면 디렉터리를
 * 더하는 사람이 빠뜨리는 순간 조용히 다시 실린다.
 *
 * 새로 설치되면 속성 없이 생기므로 앱이 뜰 때마다 다시 건다. 멱등이다.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun excludeAppDataFromIcloudBackup(appLog: AppLog): Unit = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    val documents: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = error.ptr,
    ) ?: return@memScoped appLog.sendFailure("문서 디렉터리를 찾지 못했습니다", error.value)

    if (!documents.setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = error.ptr)) {
        appLog.sendFailure("iCloud 백업 제외를 걸지 못했습니다", error.value)
    }
}

private fun AppLog.sendFailure(message: String, error: NSError?) {
    sendErrorLog(
        ErrorLog(
            "IcloudBackupExclusion",
            message,
            IllegalStateException(error?.localizedDescription ?: message),
        ),
    )
}
