package com.kintmin.domain.data_migration.model

/**
 * 그 파일 하나를 받을 수 없다(보내는 기기에 없거나 받은 크기가 다르다). 연결 실패와 달리 이전을 멈추지
 * 않고 그 파일을 가리키는 곡만 실패로 친다.
 */
class MigrationFileUnavailableException(fileName: String, cause: Throwable? = null) :
    Exception("받을 수 없는 파일: $fileName", cause)
