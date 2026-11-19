package com.kintmin.data.file_share

import com.kintmin.data.data_migration.localDeviceName
import com.kintmin.data.data_migration.toMigrationInstanceName
import com.kintmin.domain.file_share.PcUploadHandler
import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import com.kintmin.domain.user.repository.UserRepository
import com.kintmin.log.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/** 앱이 서스펜드되면 서버와 광고가 함께 멈춘다. 복귀할 때 되살린다. */
internal fun createIosFileShareServerRepository(
    appLog: AppLog,
    receiveKeepAlive: ReceiveKeepAlive,
    userRepository: UserRepository,
): FileShareServerRepository {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val repository = FileShareServerRepository(
        scope = scope,
        appLog = appLog,
        uploadTempDir = NSTemporaryDirectory(),
        receiveKeepAlive = receiveKeepAlive,
        advertiser = BonjourAdvertiser(scope, appLog) {
            toMigrationInstanceName(localDeviceName(), userRepository.getUserId().getOrNull())
        },
    )
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationWillEnterForegroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> repository.resumeAfterSuspension() }
    return repository
}

/** 이 기기가 재생하지 못하는 형식의 PC 업로드는 받기 전에 거절한다. */
internal class PlayableOnlyPcFileShareServerRepository(
    private val delegate: PcFileShareServerRepository,
) : PcFileShareServerRepository by delegate {

    override fun start(handler: PcUploadHandler) = delegate.start(PlayableOnlyPcUploadHandler(handler))
}
