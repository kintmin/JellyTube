package com.kintmin.presentation.ui.setting.data_migration_receive

data class SettingDataMigrationReceiveUiState(
    val step: DataMigrationReceiveStep = DataMigrationReceiveStep.Idle,
    val code: String = "",
) {
    val canConnect: Boolean get() = code.length == CODE_LENGTH && step is DataMigrationReceiveStep.CodeInput

    companion object {
        const val CODE_LENGTH = 6

        fun getMock() = SettingDataMigrationReceiveUiState(
            step = DataMigrationReceiveStep.Transferring(receivedBytes = 120_000_000, totalBytes = 480_000_000),
        )
    }
}

sealed interface DataMigrationReceiveStep {
    data object Idle : DataMigrationReceiveStep
    data object Searching : DataMigrationReceiveStep
    data object NotFound : DataMigrationReceiveStep
    data class CodeInput(val errorMessage: String? = null) : DataMigrationReceiveStep
    data object Pairing : DataMigrationReceiveStep
    data class Transferring(
        val receivedBytes: Long,
        val totalBytes: Long,
        val redownloadedCount: Int = 0,
        val redownloadTotal: Int = 0,
    ) : DataMigrationReceiveStep {
        val isRedownloading: Boolean get() = redownloadedCount < redownloadTotal

        val fraction: Float get() = if (totalBytes == 0L) 0f else receivedBytes.toFloat() / totalBytes
    }
    data class Completed(
        val newAudioMediaCount: Int,
        val newPlaylistCount: Int,
        val redownloadedCount: Int = 0,
        val excludedAudioMediaNames: List<String> = emptyList(),
    ) : DataMigrationReceiveStep
    data class Failed(val message: String) : DataMigrationReceiveStep
}
