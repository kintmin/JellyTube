package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.log.secret

/** 실패 원인을 볼 수 있게 원본 주소와 파일 정보를 싣는다. [secretIds] 에 든 곡은 이름과 주소를 가린다. */
internal fun MigrationAudioMedia.logText(secretIds: Set<Int>, sizeBytes: Long? = null): String =
    "name=${secret(name, id in secretIds)} source=${secret(source, id in secretIds)} ext=${audioFileName.substringAfterLast('.', missingDelimiterValue = "")} " +
        "duration=${rawAudioDurationSeconds}s" + (sizeBytes?.let { " size=$it" } ?: "")

/** 실패한 곡 이름 목록은 수천 줄이 될 수 있어 사유별 개수만 싣는다. 곡별 실패는 따로 남는다. */
internal fun MigrationSummary.logText(): String =
    "새 곡=$newAudioMediaCount 새 재생목록=$newPlaylistCount 다시 받음=$redownloadedCount " +
        "실패=${failures.groupingBy { it.reason }.eachCount()}"
