package com.bodylens.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_entries")
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMs: Long,
    val frontPhotoUri: String?,
    val sidePhotoUri: String?,
    val heightCm: Double,
    val neckCm: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val chestCm: Double?,
    val thighCm: Double?,
    val bodyFatPercent: Double?,
)
