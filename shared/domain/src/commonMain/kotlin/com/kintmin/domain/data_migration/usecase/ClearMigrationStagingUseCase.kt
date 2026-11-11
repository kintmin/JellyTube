package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.data_migration.repository.DataMigrationRepository

/** 중단한 이전의 임시 파일을 지운다. 지우면 다음 이전은 처음부터 받는다. */
class ClearMigrationStagingUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
) {
    suspend operator fun invoke(): Result<Unit> = dataMigrationRepository.clearStaging()
}
