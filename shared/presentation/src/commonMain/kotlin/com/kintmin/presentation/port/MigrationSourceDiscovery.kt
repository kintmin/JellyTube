package com.kintmin.presentation.port

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.presentation.port.model.MigrationSource

/** 같은 네트워크에서 데이터를 보내는 기기를 찾는다. */
interface MigrationSourceDiscovery {

    /** 이전 세션을 연 기기들. 로컬 네트워크 권한이 거부돼도 오류 없이 빈 목록이다 — OS 가 알려 주지 않는다. */
    suspend fun discover(): List<MigrationSource>

    /** 고른 기기의 주소. 그 사이 광고를 내렸으면 null. */
    suspend fun resolve(source: MigrationSource): MigrationEndpoint?
}
