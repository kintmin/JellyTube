package com.kintmin.domain.data_migration.model

/**
 * 그 파일 하나를 받을 수 없다(보내는 기기에 없거나 받은 크기가 다르다). 이전 전체가 아니라 그 파일을
 * 가리키는 곡만 실패로 친다. 연결이 끊긴 것과 구분해야 Wi-Fi 가 잠깐 끊겼을 때 남은 곡이 전부 실패로
 * 찍히지 않는다.
 */
class MigrationFileUnavailableException(fileName: String, cause: Throwable? = null) :
    Exception("받을 수 없는 파일: $fileName", cause)
