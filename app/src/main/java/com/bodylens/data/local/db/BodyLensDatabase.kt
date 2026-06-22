package com.bodylens.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProgressEntity::class], version = 1, exportSchema = false)
abstract class BodyLensDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
