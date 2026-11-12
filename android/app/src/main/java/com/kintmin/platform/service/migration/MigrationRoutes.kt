package com.kintmin.platform.service.migration

import com.kintmin.data.data_migration.toManifestDto
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.usecase.ExportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.ResolveMigrationFilePathUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.MigrationFinishRequest
import com.kintmin.fileshare.MigrationPairRequest
import com.kintmin.fileshare.MigrationPairResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.io.File

/**
 * 세션이 없으면 모든 경로가 404 다. 이전 화면을 켜 두지 않은 기기에서는 엔드포인트가 있다는 것도
 * 드러내지 않는다.
 */
internal fun Route.migrationRoutes(
    currentSession: () -> MigrationSession?,
    cleanupAnomalousData: CleanupAnomalousDataUseCase,
    exportMigrationData: ExportMigrationDataUseCase,
    resolveMigrationFilePath: ResolveMigrationFilePathUseCase,
) {
    post(FileShareConstants.MIGRATION_PAIR_PATH) {
        val session = currentSession() ?: return@post call.respond(HttpStatusCode.NotFound)
        val request = runCatching { call.receive<MigrationPairRequest>() }
            .getOrElse { return@post call.respond(HttpStatusCode.BadRequest) }
        val token = session.pair(request.code, call.request.origin.remoteAddress, request.deviceName)
            ?: return@post call.respond(HttpStatusCode.Unauthorized)
        call.respond(MigrationPairResponse(token))
    }

    get(FileShareConstants.MIGRATION_MANIFEST_PATH) {
        val session = call.authorizedSession(currentSession) ?: return@get
        // 받는 쪽이 연결 실패와 구분해 "보내는 기기의 정리 실패"로 보여 준다.
        cleanupAnomalousData().onFailure { return@get call.respond(HttpStatusCode.ServiceUnavailable) }
        exportMigrationData()
            .onSuccess { export ->
                session.allowFiles(export.files.map { it.file.kind.name to it.file.name }.toSet())
                call.respond(export.toManifestDto())
            }
            .onFailure { call.respond(HttpStatusCode.InternalServerError) }
    }

    post(FileShareConstants.MIGRATION_FINISH_PATH) {
        val session = call.authorizedSession(currentSession) ?: return@post
        val report = runCatching { call.receive<MigrationFinishRequest>() }
            .getOrElse { return@post call.respond(HttpStatusCode.BadRequest) }
        session.finish(report)
        call.respond(HttpStatusCode.OK)
    }

    get("${FileShareConstants.MIGRATION_FILES_PATH}/{kind}/{name}") {
        val session = call.authorizedSession(currentSession) ?: return@get
        val kind = call.parameters["kind"].orEmpty()
        val name = call.parameters["name"].orEmpty()
        if (!session.isFileAllowed(kind, name)) return@get call.respond(HttpStatusCode.NotFound)

        // 세션 없음(404)과 구분해야 받는 쪽이 이 파일만 빼고 이어 간다.
        val file = resolveMigrationFilePath(MigrationFile(MigrationFileKind.valueOf(kind), name))
            .getOrNull()
            ?.let(::File)
            ?.takeIf { it.isFile }
            ?: return@get call.respond(HttpStatusCode.Gone)
        call.respondFile(file)
    }
}

/** 인가되지 않았으면 응답까지 보내고 null 을 돌려준다. */
private suspend fun ApplicationCall.authorizedSession(currentSession: () -> MigrationSession?): MigrationSession? {
    val session = currentSession() ?: return null.also { respond(HttpStatusCode.NotFound) }
    val token = request.header(HttpHeaders.Authorization)?.removePrefix(BEARER_PREFIX)
    if (!session.isAuthorized(token, request.origin.remoteAddress)) return null.also { respond(HttpStatusCode.Unauthorized) }
    return session
}

private const val BEARER_PREFIX = "Bearer "
