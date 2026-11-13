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
    /** 업로드 성공 항목 아티스트 일괄 변경 HTTP POST endpoint */
    const val HTTP_BULK_ARTIST_PATH = "/upload/artist"
    /** 업로드 성공 항목 썸네일 일괄 변경 HTTP POST endpoint */
    const val HTTP_BULK_THUMBNAIL_PATH = "/upload/thumbnail"
    /** 바이너리 업로드 시 파일명 전달 헤더 */
    const val HEADER_FILE_NAME = "X-File-Name"
    /** 썸네일 일괄 변경 대상 audioMediaId 목록 전달 헤더 */
    const val HEADER_AUDIO_MEDIA_IDS = "X-Audio-Media-Ids"

    /** 데이터 이전 페어링 HTTP POST endpoint. 화면에 띄운 코드를 토큰으로 바꾼다 */
    const val MIGRATION_PAIR_PATH = "/migration/pair"
    /** 데이터 이전 manifest HTTP GET endpoint */
    const val MIGRATION_MANIFEST_PATH = "/migration/manifest"
    /** 데이터 이전 파일 HTTP GET endpoint. 뒤에 `/{kind}/{name}` 이 붙는다 */
    const val MIGRATION_FILES_PATH = "/migration/files"
    /** 데이터 이전 완료 알림 HTTP POST endpoint. 받는 기기가 끝나면 보내는 기기에 결과를 알린다 */
    const val MIGRATION_FINISH_PATH = "/migration/finish"
    /** 이전 프로토콜 버전. 양쪽이 다르면 받는 쪽이 이전을 거부한다 */
    const val MIGRATION_FORMAT_VERSION = 1
    /**
     * 데이터 이전 세션이 열려 있는 동안만 광고하는 mDNS 서비스 타입. [NSD_SERVICE_TYPE] 과 나눠야
     * PC 업로드(Desktop)가 이전 중인 기기를 찾지 못한다. 인스턴스 이름은 보내는 기기 이름이다
     */
    const val MIGRATION_NSD_SERVICE_TYPE = "_jellytube-mig._tcp."
    /** 이전 서비스 TXT 레코드에서 [MIGRATION_FORMAT_VERSION] 을 담는 키 */
    const val MIGRATION_TXT_FORMAT_VERSION = "v"
}
