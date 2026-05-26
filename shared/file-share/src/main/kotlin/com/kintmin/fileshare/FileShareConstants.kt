package com.kintmin.fileshare

object FileShareConstants {
    /** Android NSD로 광고할 mDNS 서비스 타입 */
    const val NSD_SERVICE_TYPE = "_jellytube._tcp."
    /** NSD 서비스 이름 (discovery 시 검색 기준) */
    const val NSD_SERVICE_NAME = "JellyTubeFileShare"
    /** Ktor 서버 포트 */
    const val DEFAULT_PORT = 52847
    /** 파일 업로드 HTTP POST endpoint */
    const val HTTP_UPLOAD_PATH = "/upload"
    /** WebSocket 서버 상태 endpoint */
    const val WS_STATUS_PATH = "/ws/status"
    /** multipart 필드 이름 */
    const val MULTIPART_FIELD_AUDIO = "audio"
    /** 바이너리 업로드 시 파일명 전달 헤더 */
    const val HEADER_FILE_NAME = "X-File-Name"
}
