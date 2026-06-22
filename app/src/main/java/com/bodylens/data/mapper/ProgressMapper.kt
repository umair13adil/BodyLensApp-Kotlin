package com.bodylens.data.mapper

import com.bodylens.data.local.db.ProgressEntity
import com.bodylens.domain.model.BodyMeasurements
import com.bodylens.domain.model.ProgressEntry

fun ProgressEntity.toDomain() = ProgressEntry(
    id = id,
    createdAtEpochMs = createdAtEpochMs,
    frontPhotoUri = frontPhotoUri,
    sidePhotoUri = sidePhotoUri,
    measurements = BodyMeasurements(
        heightCm = heightCm,
        neckCm = neckCm,
        waistCm = waistCm,
        hipCm = hipCm,
        chestCm = chestCm,
        thighCm = thighCm,
    ),
    bodyFatPercent = bodyFatPercent,
)

fun ProgressEntry.toEntity() = ProgressEntity(
    id = id,
    createdAtEpochMs = createdAtEpochMs,
    frontPhotoUri = frontPhotoUri,
    sidePhotoUri = sidePhotoUri,
    heightCm = measurements.heightCm,
    neckCm = measurements.neckCm,
    waistCm = measurements.waistCm,
    hipCm = measurements.hipCm,
    chestCm = measurements.chestCm,
    thighCm = measurements.thighCm,
    bodyFatPercent = bodyFatPercent,
)
