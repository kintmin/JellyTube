package com.kintmin.presentation.port

import com.kintmin.domain.data_migration.model.MigrationEndpoint

/** 같은 네트워크에서 데이터를 보내는 기기를 찾는다. */
interface MigrationSourceDiscovery {

    /** 못 찾으면 null. 로컬 네트워크 권한이 거부돼도 오류 없이 null 이다 — OS 가 알려 주지 않는다. */
    suspend fun discover(): MigrationEndpoint?
}
