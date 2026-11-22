package com.kintmin.data.file_share

import com.kintmin.data.file_share.migration.MigrationSession
import com.kintmin.data.file_share.migration.migrationRoutes
import com.kintmin.domain.data_migration.MigrationSendHandler
import com.kintmin.domain.file_share.PcUploadHandler
import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.fileshare.FileShareResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.routing

internal fun Application.fileShareModule(
    pcUploadHandler: PcUploadHandler,
    uploadTempDir: String,
    receiveKeepAlive: ReceiveKeepAlive,
    currentMigrationSession: () -> MigrationSession?,
    migrationSendHandler: MigrationSendHandler,
) {
    installFileShareContentNegotiation()
    val stagedUploads = StagedUploads()
    monitor.subscribe(ApplicationStopped) { stagedUploads.clear() }
    routing {
        migrationRoutes(currentMigrationSession, migrationSendHandler)

        pcUploadRoutes(currentMigrationSession) {
            pcUploadEndpoints(
                handler = pcUploadHandler,
                uploadTempDir = uploadTempDir,
                receiveKeepAlive = receiveKeepAlive,
                stagedUploads = stagedUploads,
            )
        }
    }
}

/** 여기 묶은 PC 경로는 이전 세션이 열려 있는 동안 409 다. PC 가 바꾸는 경로는 모두 여기 둔다. */
private fun Route.pcUploadRoutes(currentMigrationSession: () -> MigrationSession?, build: Route.() -> Unit) {
    createChild(PcUploadRouteSelector).apply {
        install(RejectWhileMigrating) { this.currentMigrationSession = currentMigrationSession }
        build()
    }
}

private object PcUploadRouteSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent

    override fun toString(): String = "(pc upload)"
}

private class RejectWhileMigratingConfig {
    var currentMigrationSession: () -> MigrationSession? = { null }
}

private val RejectWhileMigrating = createRouteScopedPlugin("RejectWhileMigrating", ::RejectWhileMigratingConfig) {
    val currentMigrationSession = pluginConfig.currentMigrationSession
    onCall { call ->
        if (currentMigrationSession() == null) return@onCall
        call.discardBody()
        call.respond(HttpStatusCode.Conflict, FileShareResponse(success = false, message = "다른 기기와 데이터 이전 중입니다."))
    }
}
