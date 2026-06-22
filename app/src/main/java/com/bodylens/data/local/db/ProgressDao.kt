package com.bodylens.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress_entries ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Insert
    suspend fun insert(entity: ProgressEntity): Long

    @Query("DELETE FROM progress_entries")
    suspend fun deleteAll()
}
