package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "STEP")
data class StepEntity(
    @PrimaryKey val rawCreatedTime: Long,
    val stepSensor: Long,
)